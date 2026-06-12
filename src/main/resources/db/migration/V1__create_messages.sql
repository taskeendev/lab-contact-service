CREATE TABLE messages (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(255)  NOT NULL,
    message    VARCHAR(2000) NOT NULL,
    read_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- กล่องขาเข้าเรียงใหม่→เก่า
CREATE INDEX idx_messages_created ON messages (created_at DESC);
