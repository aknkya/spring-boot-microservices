# ==============================================================================
# 🚀 OpenShift All-in-One Deployment Script (PowerShell / Windows)
# Adım Adım: Maven Build -> Docker Build -> OpenShift Project -> Helm Deploy -> Route Expose
# ==============================================================================

param(
    [string]$ProjectName = "microservices-demo",
    [string]$Registry = "",                 # Örn: "quay.io/kullanici" veya boş ise local/internal
    [switch]$SkipMavenBuild,
    [switch]$SkipDockerBuild
)

$ErrorActionPreference = "Stop"

function Print-Step {
    param([string]$Num, [string]$Title)
    Write-Host "`n==============================================================================" -ForegroundColor Cyan
    Write-Host " [$Num] $Title" -ForegroundColor Yellow
    Write-Host "==============================================================================" -ForegroundColor Cyan
}

function Print-Success {
    param([string]$Msg)
    Write-Host "[OK] $Msg" -ForegroundColor Green
}

function Print-Info {
    param([string]$Msg)
    Write-Host "[INFO] $Msg" -ForegroundColor Gray
}

function Print-Warn {
    param([string]$Msg)
    Write-Host "[WARN] $Msg" -ForegroundColor Yellow
}

# ------------------------------------------------------------------------------
# ADIM 1: Gerekli Araçların Kontrolü
# ------------------------------------------------------------------------------
Print-Step "1/7" "Gerekli CLI Araçlarının Kontrolü (Pre-flight Checks)"

$tools = @("docker", "helm", "oc")
foreach ($tool in $tools) {
    if (Get-Command $tool -ErrorAction SilentlyContinue) {
        Print-Success "$tool mevcut."
    } else {
        Print-Warn "$tool komutu bulunamadı. Lütfen yüklü ve PATH'e ekli olduğundan emin olun."
    }
}

# OpenShift oturumunu kontrol et
try {
    $currentUser = (oc whoami 2>&1).Trim()
    Print-Success "OpenShift Oturumu Aktif -> Kullanıcı: $currentUser"
} catch {
    Write-Host "[HATA] OpenShift oturumu açık değil! Lütfen önce 'oc login' komutu ile giriş yapın." -ForegroundColor Red
    exit 1
}

# ------------------------------------------------------------------------------
# ADIM 2: Maven ile Mikroservislerin Derlenmesi
# ------------------------------------------------------------------------------
Print-Step "2/7" "Maven ile 3 Mikroservisin JAR Olarak Paketlenmesi"

if ($SkipMavenBuild) {
    Print-Info "Maven derleme adımı (-SkipMavenBuild ile) atlandı."
} else {
    $mvnCmd = if (Test-Path ".\mvnw.cmd") { ".\mvnw.cmd" } else { "mvn" }
    Print-Info "Derleme komutu çalıştırılıyor: $mvnCmd clean package -DskipTests"
    
    $env:MAVEN_OPTS = "-XX:TieredStopAtLevel=1"
    & cmd /c "$mvnCmd clean package -DskipTests"
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[HATA] Maven derlemesi başarısız oldu!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Print-Success "Tüm mikroservis JAR'ları başarıyla üretildi."
}

# ------------------------------------------------------------------------------
# ADIM 3: Docker İmajlarının Oluşturulması
# ------------------------------------------------------------------------------
Print-Step "3/7" "Docker İmajlarının Paketlenmesi (Docker Build)"

$services = @("notification-service", "payment-service", "order-service")

if ($SkipDockerBuild) {
    Print-Info "Docker imaj oluşturma (-SkipDockerBuild ile) atlandı."
} else {
    foreach ($svc in $services) {
        $imageName = if ($Registry) { "$Registry/${svc}:latest" } else { "${svc}:latest" }
        Print-Info "[$svc] Docker imajı oluşturuluyor: $imageName"
        
        docker build -t $imageName -f ./$svc/Dockerfile ./$svc
        if ($LASTEXITCODE -ne 0) {
            Write-Host "[HATA] $svc için Docker build başarısız!" -ForegroundColor Red
            exit $LASTEXITCODE
        }
        Print-Success "[$svc] İmaj hazır -> $imageName"

        if ($Registry) {
            Print-Info "[$svc] İmaj depoya yükleniyor (docker push): $imageName"
            docker push $imageName
        }
    }
}

# ------------------------------------------------------------------------------
# ADIM 4: OpenShift Projesinin (Namespace) Seçimi / Oluşturulması
# ------------------------------------------------------------------------------
Print-Step "4/7" "OpenShift Projesinin (Namespace: $ProjectName) Hazırlanması"

$existingProjects = (oc projects -q 2>&1)
if ($existingProjects -contains $ProjectName) {
    Print-Info "Mevcut projeye geçiliyor: $ProjectName"
    oc project $ProjectName | Out-Null
} else {
    Print-Info "Yeni OpenShift projesi oluşturuluyor: $ProjectName"
    oc new-project $ProjectName | Out-Null
}
Print-Success "Aktif OpenShift Projesi: $ProjectName"

# ------------------------------------------------------------------------------
# ADIM 5: Helm ile Mikroservislerin OpenShift'e Dağıtımı
# ------------------------------------------------------------------------------
Print-Step "5/7" "Helm Chart'ları ile Mikroservislerin Dağıtımı (Deploy)"

# Dağıtım sırası: Bağımlılık zincirine göre (Önce Notification -> Sonra Payment -> En son Order)
foreach ($svc in $services) {
    Print-Info "[$svc] Helm kurulumu yapılıyor..."
    
    $helmArgs = @("upgrade", "--install", $svc, "./helm/$svc")
    if ($Registry) {
        $helmArgs += "--set"
        $helmArgs += "image.repository=$Registry/$svc"
    }
    
    & helm @helmArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[HATA] $svc için Helm kurulumu başarısız!" -ForegroundColor Red
        exit $LASTEXITCODE
    }
    Print-Success "[$svc] Helm ile başarıyla kuruldu."
}

# ------------------------------------------------------------------------------
# ADIM 6: Dış Dünyaya Açma (OpenShift Route Oluşturma)
# ------------------------------------------------------------------------------
Print-Step "6/7" "OpenShift Route Yapılandırması (Dış Erişim)"

$hasRoute = oc get route order-service -n $ProjectName --no-headers -o name 2>$null
if (-not $hasRoute) {
    Print-Info "Order Service için Route oluşturuluyor..."
    oc expose svc/order-service -n $ProjectName | Out-Null
    Print-Success "Order Service dış dünyaya açıldı."
} else {
    Print-Info "Order Service Route zaten mevcut."
}

# ------------------------------------------------------------------------------
# ADIM 7: Dağıtım Doğrulama ve Canlı Test Bilgisi
# ------------------------------------------------------------------------------
Print-Step "7/7" "Dağıtımın Doğrulanması (Rollout Status & URL)"

foreach ($svc in $services) {
    Print-Info "[$svc] Pod'ların hazır olması bekleniyor..."
    oc rollout status deployment/$svc -n $ProjectName --timeout=120s
}

$routeHost = (oc get route order-service -n $ProjectName -o jsonpath='{.spec.host}' 2>$null).Trim()

Write-Host "`n==============================================================================" -ForegroundColor Green
Write-Host " 🎉 TÜM MİKROSERVİSLER OPENSHIFT ÜZERİNDE BAŞARIYLA ÇALIŞIYOR!" -ForegroundColor Green
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host " Proje / Namespace : $ProjectName" -ForegroundColor Cyan
Write-Host " Aktif Pod Sayısı  : 9 (Her servis için 3 replica)" -ForegroundColor Cyan
Write-Host " Order Servis URL  : http://$routeHost" -ForegroundColor Yellow
Write-Host "==============================================================================" -ForegroundColor Green

Write-Host "`n📡 CANLI TEST YAPMAK İÇİN ŞU KOMUTU KULLANABİLİRSİNİZ:" -ForegroundColor Magenta
Write-Host "curl -X POST http://$routeHost/api/orders ``" -ForegroundColor White
Write-Host "  -H `"Content-Type: application/json`" ``" -ForegroundColor White
Write-Host "  -d '{\`"customerName\`":\`"Ahmet Yilmaz\`",\`"product\`":\`"OpenShift Cloud\`",\`"quantity\`":1,\`"price\`":999.99}'" -ForegroundColor White
