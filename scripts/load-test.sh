#!/usr/bin/env bash

# Microservices Load Testing Script (Bash / cURL)
# Target: Order Service (POST /api/orders)
# Goal: 100+ requests in 30 seconds

TARGET_URL="${1:-http://localhost:8080/api/orders}"
DURATION_SEC=30
TARGET_REQUESTS=120

echo "=========================================================="
echo " Starting Microservices Load Test"
echo " Target URL     : $TARGET_URL"
echo " Duration Limit : $DURATION_SEC seconds"
echo " Target Count   : $TARGET_REQUESTS requests"
echo "=========================================================="

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION_SEC))

TOTAL_REQ=0
SUCCESS_REQ=0
FAILED_REQ=0

PRODUCTS=("Laptop" "Smartphone" "Wireless Headphones" "Mechanical Keyboard" "Gaming Monitor")
CUSTOMERS=("Ahmet Yilmaz" "Ayse Demir" "Mehmet Kaya" "Fatma Celik" "Can Ozkan")

while [ $(date +%s) -lt $END_TIME ] && [ $TOTAL_REQ -lt $TARGET_REQUESTS ]; do
    TOTAL_REQ=$((TOTAL_REQ + 1))
    
    # Pick random product and customer
    PROD=${PRODUCTS[$RANDOM % ${#PRODUCTS[@]}]}
    CUST=${CUSTOMERS[$RANDOM % ${#CUSTOMERS[@]}]}
    QTY=$(( (RANDOM % 5) + 1 ))
    PRICE=$(( (RANDOM % 500) + 50 )).99

    PAYLOAD=$(cat <<EOF
{
  "customerName": "$CUST",
  "product": "$PROD",
  "quantity": $QTY,
  "price": $PRICE
}
EOF
)

    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$TARGET_URL" \
        -H "Content-Type: application/json" \
        -d "$PAYLOAD")

    if [ "$HTTP_STATUS" -eq 200 ] || [ "$HTTP_STATUS" -eq 201 ]; then
        SUCCESS_REQ=$((SUCCESS_REQ + 1))
        echo "[$TOTAL_REQ] HTTP $HTTP_STATUS - Order placed for $CUST ($PROD x$QTY)"
    else
        FAILED_REQ=$((FAILED_REQ + 1))
        echo "[$TOTAL_REQ] HTTP $HTTP_STATUS (FAILED) - Order failed for $CUST"
    fi

    # Small sleep ~200ms to distribute 100+ requests evenly over 30s
    sleep 0.2
done

ELAPSED_SEC=$(( $(date +%s) - START_TIME ))
[ $ELAPSED_SEC -eq 0 ] && ELAPSED_SEC=1
RPS=$(awk "BEGIN {printf \"%.2f\", $TOTAL_REQ / $ELAPSED_SEC}")

echo "=========================================================="
echo " Load Test Summary"
echo " Elapsed Time      : ${ELAPSED_SEC}s"
echo " Total Requests    : $TOTAL_REQ"
echo " Successful (2xx)  : $SUCCESS_REQ"
echo " Failed            : $FAILED_REQ"
echo " Requests / sec    : $RPS req/s"
echo "=========================================================="
