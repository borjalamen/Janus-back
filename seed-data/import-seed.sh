#!/bin/bash
# =============================================================
# Script de importación de datos de prueba en MongoDB (janusdb)
# Uso: bash import-seed.sh [MONGO_URI]
# Si no se pasa URI, usa localhost:27017
# =============================================================

MONGO_URI="${1:-mongodb://localhost:27017}"
DB="janusdb"
DIR="$(dirname "$0")"

collections=(
  "users"
  "departments"
  "projects"
  "estimaciones"
  "formations"
  "logbook"
  "herramientas"
  "infraestructura"
  "jenkins"
  "planning"
  "peticiones_tareas"
  "procedures"
  "steps"
  "scrum_sprints"
  "scrum_tasks"
  "media_videos"
  "documents"
  "join_requests"
  "counters"
  "parametrization"
)

for col in "${collections[@]}"; do
  file="$DIR/$col.json"
  if [ -f "$file" ]; then
    echo "Importando colección: $col ..."
    mongoimport --uri "$MONGO_URI" --db "$DB" --collection "$col" \
      --file "$file" --jsonArray --drop
  else
    echo "AVISO: $file no encontrado, omitiendo."
  fi
done

echo ""
echo "Importación completada."
