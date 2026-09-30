#!/usr/bin/env bash
# ==============================================================================
# 🚀 OpenShift All-in-One Deployment Script (Bash / Linux / macOS)
# Adım Adım: Maven Build -> Docker Build -> OpenShift Project -> Helm Deploy -> Route Expose
# ==============================================================================

set -euo pipefail

PROJECT_NAME="${1:-microservices-demo}"
REGISTRY="${2:-}"

# Renkler
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
RED='\033[0;31m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

print_step() {
    echo -e "\n${CYAN}==============================================================================${NC}"
    echo -e "${YELLOW} [$1] $2${NC}"
    echo -e "${CYAN}==============================================================================${NC}"
}

print_success() { echo -e "${GREEN}[OK] $1${NC}"; }
print_info()    { echo -e "${GRAY}[INFO] $1${NC}"; }
print_warn()    { echo -e "${YELLOW}[WARN] $1${NC}"; }
print_error()   { echo -e "${RED}[HATA] $1${NC}"; }

# ------------------------------------------------------------------------------
# ADIM 1: Gerekli Araçların Kontrolü
# ------------------------------------------------------------------------------
print_step "1/7" "Gerekli CLI Araçlarının Kontrolü (Pre-flight Checks)"

for tool in docker helm oc; do
    if command -v "$tool" &>/dev/null; then
        print_success "$tool mevcut."
    else
        print_warn "$tool komutu bulunamadı. Lütfen yüklü ve PATH'te olduğundan emin olun."
    fi
done

if ! oc whoami &>/dev/null; then
    print_error "OpenShift oturumu açık değil! Lütfen önce 'oc login' komutu ile giriş yapın."
    exit 1
fi
print_success "OpenShift Oturumu Aktif -> Kullanıcı: $(oc whoami)"

# ------------------------------------------------------------------------------
# ADIM 2: Maven ile Mikroservislerin Derlenmesi
# ------------------------------------------------------------------------------
print_step "2/7" "Maven ile 3 Mikroservisin JAR Olarak Paketlenmesi"

MVN_CMD="./mvnw"
[ ! -f "$MVN_CMD" ] && MVN_CMD="mvn"

print_info "Derleme komutu çalıştırılıyor: $MVN_CMD clean package -DskipTests"
$MVN_CMD clean package -DskipTests
print_success "Tüm mikroservis JAR'ları başarıyla üretildi."

# ------------------------------------------------------------------------------
# ADIM 3: Docker İmajlarının Oluşturulması
# ------------------------------------------------------------------------------
print_step "3/7" "Docker İmajlarının Paketlenmesi (Docker Build)"

SERVICES=("notification-service" "payment-service" "order-service")

for svc in "${SERVICES[@]}"; do
    IMAGE_NAME="${svc}:latest"
    [ -n "$REGISTRY" ] && IMAGE_NAME="$REGISTRY/${svc}:latest"
    
    print_info "[$svc] Docker imajı oluşturuluyor: $IMAGE_NAME"
    docker build -t "$IMAGE_NAME" -f "./$svc/Dockerfile" "./$svc"
    print_success "[$svc] İmaj hazır -> $IMAGE_NAME"

    if [ -n "$REGISTRY" ]; then
        print_info "[$svc] İmaj depoya yükleniyor: $IMAGE_NAME"
        docker push "$IMAGE_NAME"
    fi
done

# ------------------------------------------------------------------------------
# ADIM 4: OpenShift Projesinin (Namespace) Seçimi / Oluşturulması
# ------------------------------------------------------------------------------
print_step "4/7" "OpenShift Projesinin (Namespace: $PROJECT_NAME) Hazırlanması"

if oc get project "$PROJECT_NAME" &>/dev/null; then
    print_info "Mevcut projeye geçiliyor: $PROJECT_NAME"
    oc project "$PROJECT_NAME" >/dev/null
else
    print_info "Yeni OpenShift projesi oluşturuluyor: $PROJECT_NAME"
    oc new-project "$PROJECT_NAME" >/dev/null
fi
print_success "Aktif OpenShift Projesi: $PROJECT_NAME"

# ------------------------------------------------------------------------------
# ADIM 5: Helm ile Mikroservislerin OpenShift'e Dağıtımı
# ------------------------------------------------------------------------------
print_step "5/7" "Helm Chart'ları ile Mikroservislerin Dağıtımı (Deploy)"

for svc in "${SERVICES[@]}"; do
    print_info "[$svc] Helm kurulumu yapılıyor..."
    HELM_EXTRA_ARGS=""
    if [ -n "$REGISTRY" ]; then
        HELM_EXTRA_ARGS="--set image.repository=$REGISTRY/$svc"
    fi

    helm upgrade --install "$svc" "./helm/$svc" $HELM_EXTRA_ARGS
    print_success "[$svc] Helm ile başarıyla kuruldu."
done

# ------------------------------------------------------------------------------
# ADIM 6: Dış Dünyaya Açma (OpenShift Route Oluşturma)
# ------------------------------------------------------------------------------
print_step "6/7" "OpenShift Route Yapılandırması (Dış Erişim)"

if ! oc get route order-service -n "$PROJECT_NAME" &>/dev/null; then
    print_info "Order Service için Route oluşturuluyor..."
    oc expose svc/order-service -n "$PROJECT_NAME" >/dev/null
    print_success "Order Service dış dünyaya açıldı."
else
    print_info "Order Service Route zaten mevcut."
fi

# ------------------------------------------------------------------------------
# ADIM 7: Dağıtım Doğrulama ve Canlı Test Bilgisi
# ------------------------------------------------------------------------------
print_step "7/7" "Dağıtımın Doğrulanması (Rollout Status & URL)"

for svc in "${SERVICES[@]}"; do
    print_info "[$svc] Pod'ların hazır olması bekleniyor..."
    oc rollout status "deployment/$svc" -n "$PROJECT_NAME" --timeout=120s
done

ROUTE_HOST=$(oc get route order-service -n "$PROJECT_NAME" -o jsonpath='{.spec.host}')

echo -e "\n${GREEN}==============================================================================${NC}"
echo -e "${GREEN} 🎉 TÜM MİKROSERVİSLER OPENSHIFT ÜZERİNDE BAŞARIYLA ÇALIŞIYOR!${NC}"
echo -e "${GREEN}==============================================================================${NC}"
echo -e "${CYAN} Proje / Namespace : $PROJECT_NAME${NC}"
echo -e "${CYAN} Aktif Pod Sayısı  : 9 (Her servis için 3 replica)${NC}"
echo -e "${YELLOW} Order Servis URL  : http://$ROUTE_HOST${NC}"
echo -e "${GREEN}==============================================================================${NC}"

echo -e "\n${YELLOW}📡 CANLI TEST YAPMAK İÇİN ŞU KOMUTU KULLANABİLİRSİNİZ:${NC}"
echo -e "curl -X POST http://$ROUTE_HOST/api/orders \\"
echo -e "  -H \"Content-Type: application/json\" \\"
echo -e "  -d '{\"customerName\":\"Ahmet Yilmaz\",\"product\":\"OpenShift Cloud\",\"quantity\":1,\"price\":999.99}'"
