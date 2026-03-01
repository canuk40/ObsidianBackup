#!/bin/bash

# Bulk Device Registration Script
# Register multiple devices via CSV import

API_URL="http://localhost:8080"
TOKEN="your-jwt-token"

# Check if CSV file provided
if [ -z "$1" ]; then
    echo "Usage: $0 <devices.csv>"
    echo ""
    echo "CSV Format:"
    echo "name,platform,osVersion,appVersion,userId"
    echo "John's Phone,Android,14.0,1.0.0,user-uuid-1"
    exit 1
fi

CSV_FILE="$1"

# Skip header and process each line
tail -n +2 "$CSV_FILE" | while IFS=, read -r name platform osVersion appVersion userId; do
    echo "Registering device: $name"
    
    response=$(curl -s -X POST "$API_URL/api/v1/devices/register" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"name\": \"$name\",
            \"platform\": \"$platform\",
            \"osVersion\": \"$osVersion\",
            \"appVersion\": \"$appVersion\"
        }")
    
    if echo "$response" | grep -q '"id"'; then
        device_id=$(echo "$response" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
        echo "✅ Registered: $name (ID: $device_id)"
    else
        echo "❌ Failed: $name"
        echo "   Response: $response"
    fi
    
    sleep 0.5
done

echo ""
echo "Bulk registration complete!"
