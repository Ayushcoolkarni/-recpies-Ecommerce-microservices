#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Creates all application databases on first Postgres container startup.
# Mounted at /docker-entrypoint-initdb.d/ — runs automatically as root.
# ─────────────────────────────────────────────────────────────────────────────
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE "userDB";
    CREATE DATABASE "orderServiceDB";
    CREATE DATABASE "inventoryDB";
    CREATE DATABASE "paymentDB";
    CREATE DATABASE "recipeDB";
    CREATE DATABASE "adminDB";

    GRANT ALL PRIVILEGES ON DATABASE "userDB"        TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE "orderServiceDB" TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE "inventoryDB"   TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE "paymentDB"     TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE "recipeDB"      TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE "adminDB"       TO $POSTGRES_USER;
EOSQL

echo "All databases created successfully."
