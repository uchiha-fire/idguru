use idguru::IdGuru;

#[test]
fn test_uuidv7_format() {
    let id = IdGuru::uuidv7();
    assert_eq!(id.len(), 36);
    assert_eq!(id.chars().nth(14).unwrap(), '7');
    let variant = id.chars().nth(19).unwrap();
    assert!(variant == '8' || variant == '9' || variant == 'a' || variant == 'b');
}

#[test]
fn test_uuidv7_monotonicity() {
    let mut ids = Vec::new();
    for _ in 0..5000 {
        ids.push(IdGuru::uuidv7());
    }

    for i in 1..ids.len() {
        assert!(ids[i] > ids[i - 1], "UUIDv7 must be strictly ascending");
    }
}

#[test]
fn test_uuidv4() {
    let id = IdGuru::uuidv4();
    assert_eq!(id.len(), 36);
    assert_eq!(id.chars().nth(14).unwrap(), '4');
}

#[test]
fn test_ulid() {
    let id = IdGuru::ulid();
    assert_eq!(id.len(), 26);
}

#[test]
fn test_ksuid() {
    let id = IdGuru::ksuid();
    assert_eq!(id.len(), 27);
}

#[test]
fn test_snowflake() {
    let id = IdGuru::snowflake(42);
    assert!(id.len() >= 15);
}

#[test]
fn test_nanoid() {
    let id21 = IdGuru::nanoid(21);
    assert_eq!(id21.len(), 21);

    let id32 = IdGuru::nanoid(32);
    assert_eq!(id32.len(), 32);
}

#[test]
fn test_prefixed() {
    let id = IdGuru::prefixed("inv");
    assert!(id.starts_with("inv_"));
    let raw = &id[4..];
    assert_eq!(raw.chars().nth(14).unwrap(), '7');
}

#[test]
fn test_builder_uuidv7_prefixed() {
    let gen = IdGuru::builder()
        .uuidv7()
        .prefix("usr")
        .delimiter("_")
        .monotonic(true)
        .build();

    let user_id = gen.generate();
    assert!(user_id.starts_with("usr_"));

    let bytes = gen.generate_bytes();
    assert_eq!(bytes.len(), 16);
}

#[test]
fn test_builder_snowflake() {
    let gen = IdGuru::builder()
        .snowflake(15)
        .prefix("ord")
        .build();

    let order_id = gen.generate();
    assert!(order_id.starts_with("ord_"));

    let num = gen.generate_i64();
    assert!(num > 0);
}

#[test]
fn test_inspect_uuidv7() {
    let id = IdGuru::uuidv7();
    let meta = IdGuru::inspect(&id);
    assert!(meta.is_valid);
    assert_eq!(meta.algorithm, "uuidv7");
    assert_eq!(meta.version, 7);
}

#[test]
fn test_inspect_prefixed() {
    let id = "cust_019a6b42-8e12-7a22-9f1a-b83c7d612e44";
    let meta = IdGuru::inspect(id);
    assert!(meta.is_valid);
    assert_eq!(meta.prefix.as_deref(), Some("cust"));
    assert_eq!(meta.canonical, "019a6b42-8e12-7a22-9f1a-b83c7d612e44");
    assert_eq!(meta.algorithm, "uuidv7");
}

#[test]
fn test_inspect_ulid() {
    let id = IdGuru::ulid();
    let meta = IdGuru::inspect(&id);
    assert!(meta.is_valid);
    assert_eq!(meta.algorithm, "ulid");
}

#[test]
fn test_inspect_snowflake() {
    let id = IdGuru::snowflake(12);
    let meta = IdGuru::inspect(&id);
    assert!(meta.is_valid);
    assert_eq!(meta.algorithm, "snowflake");
    assert_eq!(meta.worker_id, 12);
}

#[test]
fn test_inspect_invalid() {
    let meta = IdGuru::inspect("invalid-garbage-!@#$");
    assert!(!meta.is_valid);
    assert_eq!(meta.algorithm, "unknown");
}
