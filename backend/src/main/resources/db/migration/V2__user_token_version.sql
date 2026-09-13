-- Revocación de sesiones: el JWT lleva la versión vigente; cambiar contraseña, deshabilitar o cerrar
-- sesión incrementa la columna y todas las cookies emitidas antes dejan de ser válidas.
ALTER TABLE app_user ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;
