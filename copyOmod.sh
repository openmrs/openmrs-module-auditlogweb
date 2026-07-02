#!/bin/bash

# ==========================================
# KONFIGURACJA ZMIENNYCH
# ==========================================
PROJECT_DIR="$HOME/CFL/MSF/openmrs-module-auditlogweb"
TARGET_DIR="$PROJECT_DIR/omod/target"
CONTAINER_NAME="openmrs-distro-referenceapplication-backend-1"

# Ścieżki wewnątrz kontenera
DATA_MODULES_DIR="/openmrs/data/modules"
DISTRO_MODULES_DIR="/openmrs/distribution/openmrs_modules"
LIB_CACHE_DIR="/openmrs/data/.openmrs-lib-cache/auditlogweb"

# Wzorzec nazwy pliku do usunięcia / skopiowania
FILE_PATTERN="auditlogweb-*.omod"

echo "=========================================="
echo " Uruchamianie deploymentu modułu OMOD"
echo "=========================================="

# 1. Znalezienie najnowszego pliku .omod w katalogu target
echo "[1/5] Szukanie najnowszego pliku .omod w $TARGET_DIR..."
LATEST_OMOD=$(ls -t "$TARGET_DIR"/$FILE_PATTERN 2>/dev/null | head -n 1)

if [ -z "$LATEST_OMOD" ]; then
    echo "❌ Błąd: Nie znaleziono żadnego pliku .omod pasującego do wzorca w $TARGET_DIR."
    echo "Upewnij się, że najpierw uruchomiłeś 'mvn clean install'."
    exit 1
fi

FILENAME=$(basename "$LATEST_OMOD")
echo "✅ Znaleziono plik: $FILENAME"

# 2. Sprawdzenie czy kontener działa
if ! docker ps --format '{{.Names}}' | grep -Eq "^${CONTAINER_NAME}\$"; then
    echo "❌ Błąd: Kontener $CONTAINER_NAME nie działa."
    exit 1
fi

# 3. Usuwanie starych wersji z kontenera
echo "[2/5] Usuwanie starych wersji pliku z kontenera..."
# Używamy -u 0 (root), aby pominąć ewentualne problemy z prawami dostępu
docker exec -u 0 "$CONTAINER_NAME" sh -c "rm -f $DATA_MODULES_DIR/$FILE_PATTERN"
docker exec -u 0 "$CONTAINER_NAME" sh -c "rm -f $DISTRO_MODULES_DIR/$FILE_PATTERN"
echo "✅ Stare pliki usunięte z $DATA_MODULES_DIR oraz $DISTRO_MODULES_DIR."

# 4. Kopiowanie nowego pliku do kontenera
echo "[3/5] Kopiowanie $FILENAME do $DISTRO_MODULES_DIR w kontenerze..."
docker cp "$LATEST_OMOD" "$CONTAINER_NAME:$DISTRO_MODULES_DIR/$FILENAME"
echo "✅ Plik skopiowany pomyślnie."

# 5. Czyszczenie OpenMRS lib-cache, żeby przy starcie moduł rozpakował się ze świeżego .omod
# (bez tego classpath-loaded resourcy mogą pochodzić ze starego unpakowanego
# cache'a, mimo że .omod jest aktualny).
echo "[4/5] Czyszczenie OpenMRS lib-cache ($LIB_CACHE_DIR)..."
docker exec -u 0 "$CONTAINER_NAME" sh -c "rm -rf $LIB_CACHE_DIR"
echo "✅ Cache wyczyszczony."

echo "[5/5] Zakończono!"
echo "=========================================="
echo "Wskazówka: Aby OpenMRS załadował nowy moduł, musisz zrestartować backend."
echo "Możesz to zrobić wpisując:"
echo "docker restart $CONTAINER_NAME"