CREATE ROLE "securestatement-api" WITH LOGIN PASSWORD 'password';
ALTER DATABASE securestatement_db OWNER TO "securestatement-api";

GRANT ALL SCHEMA public TO "securestatement-api";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO "securestatement-api";
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO "securestatement-api";GRANT ALL ON SCHEMA public TO "securestatement-api";