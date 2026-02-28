#!/bin/bash
set -e

# Create prod database and user (main, already created by POSTGRES_DB/POSTGRES_USER)
# Create dev database and user with _dev suffix

DEV_DB="${POSTGRES_DB}_dev"
DEV_USER="${POSTGRES_USER}_dev"
DEV_PASSWORD="${POSTGRES_PASSWORD}_dev"

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Create dev user
    DO \$\$
    BEGIN
        IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${DEV_USER}') THEN
            CREATE ROLE ${DEV_USER} WITH LOGIN PASSWORD '${DEV_PASSWORD}';
        END IF;
    END
    \$\$;

    -- Create dev database
    SELECT 'CREATE DATABASE ${DEV_DB} OWNER ${DEV_USER}'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '${DEV_DB}')\gexec

    -- Grant privileges
    GRANT ALL PRIVILEGES ON DATABASE ${DEV_DB} TO ${DEV_USER};
EOSQL

# Connect to dev database and grant schema permissions
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$DEV_DB" <<-EOSQL
    GRANT ALL ON SCHEMA public TO ${DEV_USER};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${DEV_USER};
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ${DEV_USER};
EOSQL

echo "=== Dev database '${DEV_DB}' and user '${DEV_USER}' created successfully ==="
