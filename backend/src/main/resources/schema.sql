-- Hibernate creates/updates these tables from the entities. This reference schema
-- documents the relationships and constraints for a fresh MySQL database.
CREATE TABLE IF NOT EXISTS users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  full_name VARCHAR(120) NOT NULL,
  email VARCHAR(190) NOT NULL UNIQUE,
  phone_number VARCHAR(16),
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(160) NOT NULL,
  description TEXT NOT NULL,
  category VARCHAR(80) NOT NULL,
  event_date DATE NOT NULL,
  start_time TIME NOT NULL,
  venue VARCHAR(200) NOT NULL,
  image_url VARCHAR(1000),
  ticket_price DECIMAL(10,2) NOT NULL,
  total_tickets INT NOT NULL,
  available_tickets INT NOT NULL,
  cancelled BOOLEAN NOT NULL DEFAULT FALSE,
  organizer_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users(id),
  CONSTRAINT chk_event_inventory CHECK (total_tickets >= 0 AND available_tickets >= 0 AND available_tickets <= total_tickets),
  CONSTRAINT chk_event_price CHECK (ticket_price >= 0)
);
CREATE TABLE IF NOT EXISTS bookings (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  event_id BIGINT NOT NULL,
  ticket_quantity INT NOT NULL,
  total_price DECIMAL(10,2) NOT NULL,
  booking_status VARCHAR(20) NOT NULL,
  booking_reference VARCHAR(40) NOT NULL UNIQUE,
  booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_bookings_event FOREIGN KEY (event_id) REFERENCES events(id),
  CONSTRAINT chk_booking_quantity CHECK (ticket_quantity > 0)
);
