# Database Migrations (Flyway)

This project uses **Flyway** to manage database schema changes. This ensures that the database schema is versioned and can be consistently applied across different environments (development, staging, production).

## Key Concepts

### 1. Schema Ownership
In this project, **Flyway is the owner of the database schema**. 
- The Hibernate `ddl-auto` setting is explicitly set to `none` in `application.yaml`. 
- Developers must never allow JPA/Hibernate to automatically alter the schema.
- All structural changes (creating tables, adding columns, modifying constraints) must be done through SQL migration files.

### 2. Migration Files
Migration scripts are located in `src/main/resources/db/migration`. 
- **Naming Convention**: `V<Version>__<Description>.sql` (e.g., `V1__init.sql`).
- **Execution**: Flyway automatically scans this directory on application startup and executes any scripts that haven't been applied yet.

### 3. Baseline Support
The system is configured with `baseline-on-migrate: true`. This allows Flyway to be introduced to an existing database that was not originally managed by Flyway. It creates a `flyway_schema_history` table and starts tracking from the baseline version.

## Common Operations

### Adding a New Migration
1. Create a new `.sql` file in `src/main/resources/db/migration`.
2. Increment the version number (e.g., if the last file is `V21`, your new file should be `V22`).
3. Write the necessary DDL/DML statements.
4. Restart the application. Flyway will detect and apply the change.

### Checking Migration Status
You can check the `flyway_schema_history` table in your database to see:
- Which migrations have been applied.
- When they were applied.
- Whether they were successful.
- The checksum of the file at the time of migration.

## Important Rules
- **Never Modify Applied Migrations**: Once a migration file has been pushed and applied to an environment, do not edit it. Flyway checks checksums and will fail if an applied file is changed. If you need to fix a mistake, create a new "clean-up" migration (e.g., `V23`).
- **PostgreSQL JSONB**: Since we use PostgreSQL, many migrations involve adding or modifying JSONB columns for flexible data storage.
- **Indexes**: Performance-critical indexes should be added via migration scripts to ensure they exist in all environments.

## Current Migration History (Overview)
- `V1` - `V2`: Core constraints and fixes.
* `V3`: Feedback system.
* `V4`: Performance indexes.
* `V6`: Appraisal configuration and announcements.
* `V9`: Module feature toggles.
* `V13` - `V17`: Role expansions and reporting hierarchy fields.
* `V18`: Non-Teaching workflow system (complex tables).
* `V20`: Reviewer draft snapshots.
