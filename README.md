# Homa backend

The `kernel/` and `ie/` shared modules and the `domain` and `usecase` layers of each bounded context.

- Product: `docs/homa-product-description.md`
- Engineering rules: `docs/backend_module_discipline.md`

## Layout

One Maven source tree. Each module is a top-level package under `src/main/java`: `kernel`, `ie`, and one per
bounded context with its layers below it (`reservation.domain`, `reservation.usecase`). Tests mirror it under
`src/test/java`, with shared fixtures and port fakes in each context's `support` package.

## Build and quality gates

```
mvn verify                                  # unit tests, dependency rules (ArchUnit), coverage, mutation testing
python3 tools/quality_gates.py              # CRAP below 6; mutation 90% for kernel and domain, 80% for usecase; ratchet
python3 tools/quality_gates.py --update     # the same, and raise the ratchet in quality/ratchet.json after a pass
```

A new context is added to PIT's `targetClasses` and `targetTests` in `pom.xml`.
