#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
command -v mvn >/dev/null 2>&1 || { echo "Maven 3.9+ no está disponible en PATH"; exit 1; }
mkdir -p data/documentos
echo "TRIA Inventario: http://localhost:8080"
mvn spring-boot:run
