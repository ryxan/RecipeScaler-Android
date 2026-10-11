# Project-specific R8 rules.
# Hilt, Navigation (type-safe routes) and kotlinx.serialization ship their own consumer rules,
# so nothing is needed here yet. Add keeps only for code reached via reflection (e.g. Gson DTOs).
