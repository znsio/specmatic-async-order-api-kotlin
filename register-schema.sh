#!/bin/sh
set -e

# Check for Schema Registry URL
if [ -z "${SCHEMA_REGISTRY_URL}" ]; then
    echo 'SCHEMA_REGISTRY_URL env variable is not set. Exiting.'
    exit 1
fi

# Wait for Schema Registry
echo -n 'Waiting for Schema Registry to be ready'
until [ "$(curl -s -w '%{http_code}' -o /dev/null "${SCHEMA_REGISTRY_URL}/subjects")" -eq 200 ]; do
    sleep 5
    echo -n '.'
done
echo
echo "Schema Registry is ready at ${SCHEMA_REGISTRY_URL}"

# Process schema files
for schema_file in /schemas/*.avsc; do
    [ -f "$schema_file" ] || continue

    echo "Processing schema file: $schema_file"

    filename=$(basename "$schema_file" .avsc)

    # CUSTOM MAPPING for your case:
    # If the schema is CreateOrderReply.avsc, use topic = create-order-reply and class = order.CreateOrderReply
    if [ "$filename" = "CreateOrderReply" ]; then
        topic_name="create-order-reply"
        record_class="order.CreateOrderReply"
        subject="${topic_name}-${record_class}"
    else
        echo "Unknown schema: $filename. Skipping."
        continue
    fi

    echo "Registering schema under subject: $subject"

    # Read schema content and escape it properly
    schema_content=$(sed 's/"/\\"/g' "$schema_file" | tr -d '\n')

    # Create request body
    request_body="{\"schema\": \"${schema_content}\"}"

    # Register schema
    response=$(curl -s -X POST \
        "${SCHEMA_REGISTRY_URL}/subjects/${subject}/versions" \
        -H "Content-Type: application/vnd.schemaregistry.v1+json" \
        -d "$request_body")

    echo "Response: $response"

    if echo "$response" | grep -q "error_code"; then
        echo "❌ Error registering schema for $subject"
        echo "Error details: $response"
    else
        echo "✅ Successfully registered schema for $subject"
    fi
done

echo "✅ Schema registration completed"
