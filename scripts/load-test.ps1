# Microservices Load Testing Script (PowerShell)
# Target: Order Service (POST /api/orders)
# Goal: 100+ requests in 30 seconds

param(
    [string]$TargetUrl = "http://localhost:8080/api/orders",
    [int]$DurationSec = 30,
    [int]$TargetRequests = 120
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Starting Microservices Load Test" -ForegroundColor Cyan
Write-Host " Target URL     : $TargetUrl"
Write-Host " Duration Limit : $DurationSec seconds"
Write-Host " Target Count   : $TargetRequests requests"
Write-Host "==========================================================" -ForegroundColor Cyan

$products = @("Laptop", "Smartphone", "Wireless Headphones", "Mechanical Keyboard", "Gaming Monitor")
$customers = @("Ahmet Yilmaz", "Ayse Demir", "Mehmet Kaya", "Fatma Celik", "Can Ozkan")

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$totalReq = 0
$successReq = 0
$failedReq = 0

while ($stopwatch.Elapsed.TotalSeconds -lt $DurationSec -and $totalReq -lt $TargetRequests) {
    $totalReq++
    $prod = $products | Get-Random
    $cust = $customers | Get-Random
    $qty = (Get-Random -Minimum 1 -Maximum 6)
    $price = [math]::Round((Get-Random -Minimum 50.0 -Maximum 500.0), 2)

    $body = @{
        customerName = $cust
        product = $prod
        quantity = $qty
        price = $price
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod -Uri $TargetUrl -Method Post -Body $body -ContentType "application/json" -TimeoutSec 5 -ErrorAction Stop
        $successReq++
        Write-Host "[$totalReq] HTTP 201/200 - Order created: Order #$($response.id) for $cust ($prod x$qty) - Status: $($response.status)" -ForegroundColor Green
    }
    catch {
        $failedReq++
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "[$totalReq] HTTP $statusCode (FAILED) - Error: $($_.Exception.Message)" -ForegroundColor Red
    }

    Start-Sleep -Milliseconds 200
}

$stopwatch.Stop()
$elapsedSec = [math]::Round($stopwatch.Elapsed.TotalSeconds, 2)
if ($elapsedSec -eq 0) { $elapsedSec = 1 }
$rps = [math]::Round(($totalReq / $elapsedSec), 2)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Load Test Summary" -ForegroundColor Cyan
Write-Host " Elapsed Time      : ${elapsedSec}s"
Write-Host " Total Requests    : $totalReq"
Write-Host " Successful (2xx)  : $successReq" -ForegroundColor Green
$failColor = if ($failedReq -gt 0) { "Red" } else { "Gray" }
Write-Host " Failed            : $failedReq" -ForegroundColor $failColor
Write-Host " Requests / sec    : $rps req/s"
Write-Host "==========================================================" -ForegroundColor Cyan
