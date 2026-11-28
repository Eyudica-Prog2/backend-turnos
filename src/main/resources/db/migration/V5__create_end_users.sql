-- End-user accounts and credentials are owned by backend-turnos, the authentication authority.
CREATE TABLE jhi_user (
    id               BIGSERIAL    PRIMARY KEY,
    public_id        UUID         NOT NULL,
    login            VARCHAR(50)  NOT NULL,
    password_hash    VARCHAR(120) NOT NULL,
    first_name       VARCHAR(50),
    last_name        VARCHAR(50),
    email            VARCHAR(254) NOT NULL,
    image_url        VARCHAR(254),
    lang_key         VARCHAR(10),
    activated        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL,
    modified_at      TIMESTAMPTZ  NOT NULL,
    last_modified_by VARCHAR(50),
    CONSTRAINT uq_jhi_user_login UNIQUE (login),
    CONSTRAINT uq_jhi_user_email UNIQUE (email),
    CONSTRAINT uq_jhi_user_public_id UNIQUE (public_id),
    CONSTRAINT ck_jhi_user_login_length CHECK (char_length(login) >= 3),
    CONSTRAINT ck_jhi_user_password_hash CHECK (char_length(password_hash) >= 60)
);

CREATE TABLE jhi_authority (
    name VARCHAR(50) PRIMARY KEY,
    CONSTRAINT ck_jhi_authority_name CHECK (name LIKE 'ROLE\_%')
);

INSERT INTO jhi_authority (name) VALUES ('ROLE_USER');

CREATE TABLE jhi_user_authority (
    user_id        BIGINT      NOT NULL,
    authority_name VARCHAR(50) NOT NULL,
    CONSTRAINT pk_jhi_user_authority PRIMARY KEY (user_id, authority_name),
    CONSTRAINT fk_jhi_user_authority_user
        FOREIGN KEY (user_id) REFERENCES jhi_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_jhi_user_authority_authority
        FOREIGN KEY (authority_name) REFERENCES jhi_authority (name)
);
