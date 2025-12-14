DO $$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ticketchief_user') THEN
      PERFORM dblink_exec('dbname=postgres user=' || current_user, 'CREATE DATABASE ticketchief_user');
   END IF;
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ticketchief_event') THEN
      PERFORM dblink_exec('dbname=postgres user=' || current_user, 'CREATE DATABASE ticketchief_event');
   END IF;
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ticketchief_orders') THEN
      PERFORM dblink_exec('dbname=postgres user=' || current_user, 'CREATE DATABASE ticketchief_orders');
   END IF;
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ticketchief_payment') THEN
      PERFORM dblink_exec('dbname=postgres user=' || current_user, 'CREATE DATABASE ticketchief_payment');
   END IF;
   IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ticketchief_notification') THEN
      PERFORM dblink_exec('dbname=postgres user=' || current_user, 'CREATE DATABASE ticketchief_notification');
   END IF;
EXCEPTION WHEN undefined_function THEN
   -- dblink extension not available; fall back to plain CREATE DATABASE statements (may error if DB exists)
   BEGIN
      CREATE DATABASE ticketchief_user; EXCEPTION WHEN others THEN NULL; END;
   BEGIN
      CREATE DATABASE ticketchief_event; EXCEPTION WHEN others THEN NULL; END;
   BEGIN
      CREATE DATABASE ticketchief_orders; EXCEPTION WHEN others THEN NULL; END;
   BEGIN
      CREATE DATABASE ticketchief_payment; EXCEPTION WHEN others THEN NULL; END;
   BEGIN
      CREATE DATABASE ticketchief_notification; EXCEPTION WHEN others THEN NULL; END;
END$$;
