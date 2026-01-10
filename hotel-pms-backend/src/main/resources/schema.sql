-- Hotel PMS Database Schema

-- Users
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'RECEPTIONIST')),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Room Types
CREATE TABLE IF NOT EXISTS room_type (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    base_price NUMERIC(10,2) NOT NULL,
    active BOOLEAN DEFAULT TRUE
);

-- Rooms
CREATE TABLE IF NOT EXISTS room (
    id SERIAL PRIMARY KEY,
    room_number VARCHAR(10) UNIQUE NOT NULL,
    room_type_id INT NOT NULL REFERENCES room_type(id),
    active BOOLEAN DEFAULT TRUE
);

-- Guests
CREATE TABLE IF NOT EXISTS guest (
    id SERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    id_number VARCHAR(30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bookings
CREATE TABLE IF NOT EXISTS booking (
    id SERIAL PRIMARY KEY,
    room_id INT NOT NULL REFERENCES room(id),
    guest_id INT NOT NULL REFERENCES guest(id),
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('BOOKED','CHECKED_IN','CHECKED_OUT','CANCELLED')),
    created_by INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Stay Nights
CREATE TABLE IF NOT EXISTS stay_night (
    id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES booking(id) ON DELETE CASCADE,
    stay_date DATE NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    UNIQUE(booking_id, stay_date)
);

-- Payments
CREATE TABLE IF NOT EXISTS payment (
    id SERIAL PRIMARY KEY,
    booking_id INT NOT NULL REFERENCES booking(id),
    amount NUMERIC(10,2) NOT NULL,
    payment_method VARCHAR(10) NOT NULL CHECK (payment_method IN ('CASH','CARD')),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    user_id INT REFERENCES users(id)
);

-- Fiscal Receipts
CREATE TABLE IF NOT EXISTS fiscal_receipt (
    id SERIAL PRIMARY KEY,
    payment_id INT NOT NULL REFERENCES payment(id),
    receipt_number VARCHAR(50),
    fiscal_date TIMESTAMP,
    total_amount NUMERIC(10,2),
    status VARCHAR(20) CHECK (status IN ('OK','ERROR','STORNO'))
);

-- Fiscal Reports (Z and X reports history)
CREATE TABLE IF NOT EXISTS fiscal_report (
    id SERIAL PRIMARY KEY,
    report_type VARCHAR(10) NOT NULL CHECK (report_type IN ('Z_REPORT','X_REPORT')),
    report_number VARCHAR(50),
    fiscal_date TIMESTAMP NOT NULL,
    user_id INT NOT NULL REFERENCES users(id),
    success BOOLEAN DEFAULT TRUE,
    error_message VARCHAR(500)
);

-- Shift
CREATE TABLE IF NOT EXISTS shift (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    cash_open NUMERIC(10,2) DEFAULT 0,
    cash_close NUMERIC(10,2)
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_booking_room_id ON booking(room_id);
CREATE INDEX IF NOT EXISTS idx_booking_guest_id ON booking(guest_id);
CREATE INDEX IF NOT EXISTS idx_booking_dates ON booking(check_in_date, check_out_date);
CREATE INDEX IF NOT EXISTS idx_stay_night_date ON stay_night(stay_date);
CREATE INDEX IF NOT EXISTS idx_payment_date ON payment(payment_date);
CREATE INDEX IF NOT EXISTS idx_payment_booking_id ON payment(booking_id);
CREATE INDEX IF NOT EXISTS idx_fiscal_report_date ON fiscal_report(fiscal_date);
CREATE INDEX IF NOT EXISTS idx_fiscal_report_type ON fiscal_report(report_type);

