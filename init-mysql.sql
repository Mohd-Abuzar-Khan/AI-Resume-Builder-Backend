-- Initialize databases and grant permissions
-- Create databases if they don't exist
CREATE DATABASE IF NOT EXISTS resumade_templates;
CREATE DATABASE IF NOT EXISTS resumade_auth;
CREATE DATABASE IF NOT EXISTS resumade_resume;
CREATE DATABASE IF NOT EXISTS resumade_jobs;
CREATE DATABASE IF NOT EXISTS resumade_ai;
CREATE DATABASE IF NOT EXISTS resumade_export;
CREATE DATABASE IF NOT EXISTS resumade_notifications;

-- Grant all privileges to Z4RY user on all databases
GRANT ALL PRIVILEGES ON resumade_templates.* TO 'Z4RY'@'%';
GRANT ALL PRIVILEGES ON resumade_auth.* TO 'Z4RY'@'%';
GRANT ALL PRIVILEGES ON resumade_resume.* TO 'Z4RY'@'%';
GRANT ALL PRIVILEGES ON resumade_jobs.* TO 'Z4RY'@'%';
GRANT ALL PRIVILEGES ON resumade_ai.* TO 'Z4RY'@'%';
GRANT ALL PRIVILEGES ON resumade_export.* TO 'Z4RY'@'%';
GRANT ALL PRIVILEGES ON resumade_notifications.* TO 'Z4RY'@'%';

-- Flush privileges to apply changes
FLUSH PRIVILEGES;
