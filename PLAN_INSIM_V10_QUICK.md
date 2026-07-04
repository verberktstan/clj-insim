# InSim v10 Upgrade - Quick Checklist

**Status**: ✅ Phase 1 + Phase 2.1 COMPLETE  
**Current**: v10 enums & flags + version-aware parse context implemented  
**Target**: Full v10 support (time handling, defaults, integration testing)  
**Remaining Effort**: ~3.5 hours (Phase 2.2-2.9, Phase 3, Phase 4)

---

## 4 Implementation Phases

### ☑ Phase 1: Enums & Flags ✅ COMPLETE (30 mins, 2026-07-04)
- [x] **1.1** Rename TINY_GTH → TINY_GTM at position 8 (enum.clj) ✅
- [x] **1.2** Add TINY_PLH, TINY_IPB, TINY_LCL at positions 28-30 (enum.clj) ✅
- [x] **1.3** Add SMALL_LCL at position 10 (enum.clj) ✅
- [x] **1.4** Replace PLAYER flags position 5 `:reserved-32` → `:flexible-steer` (flags.clj) ✅
- [x] **1.5** Add CAR_CONTACT_INFO flags set with :blue, :yellow, :oob, :lag (flags.clj) ✅
- [x] **BONUS** Add comprehensive test coverage (4 new test suites, 16 assertions) ✅

**Phase 1 Status**:
- Implementation: ✅ All 5 tasks completed
- Tests: ✅ 33/33 passing (100 assertions), 0 failures
- Files Changed: 4 (enum.clj, flags.clj, enum_test.clj, flags_test.clj)
- Net Changes: +55 insertions, -4 deletions
- Backward Compatibility: ✅ No breaking changes
- Documentation: See PHASE1_FINAL_SUMMARY.md

### 🔄 Phase 2: Time Format Handling (1-2 hours)
- [x] **2.1** Track InSimVer in parse context (parse.clj) ✅ COMPLETE (2026-07-04)
- [ ] **2.2** Verify IS_OBH has time field unsigned (codecs.clj)
- [ ] **2.3** Verify IS_HLV has time field unsigned (codecs.clj)
- [ ] **2.4** Verify IS_CON has time field unsigned (codecs.clj)
- [ ] **2.5** Verify IS_RIP has ctime/ttime fields unsigned (codecs.clj)
- [ ] **2.6** Add/verify IS_UCO codec with time field (codecs.clj)
- [ ] **2.7** Add/verify IS_CSC codec with time field (codecs.clj)
- [ ] **2.8** Verify SMALL_RTP has time field (codecs.clj)
- [ ] **2.9** Add version-aware time parsing to body parsers (parse.clj)

### ☐ Phase 3: Integration & Defaults (30 mins)
- [ ] **3.1** Update default insim-version from 9 → 10 (packets.clj line 62)
- [ ] **3.2** Update documentation (README.md, feature_*.md)
- [ ] **3.3** Add version compatibility notes

### ☐ Phase 4: Testing (2-3 hours)
- [ ] **4.1** Unit tests for enum positions
- [ ] **4.2** Unit tests for codec time fields
- [ ] **4.3** Integration tests with real LFS connection
- [ ] **4.4** Backward compatibility tests (v9 if feasible)
- [ ] **4.5** Update/add example code

---

## Key Changes Summary

| Change | Type | Impact | File |
|--------|------|--------|------|
| GTH → GTM | Rename | Packet subtype | enum.clj |
| +3 TINY types | Enum | PLH, IPB, LCL requests | enum.clj |
| +1 SMALL type | Enum | LCL control | enum.clj |
| flexible-steer | Flag | Player input mode | flags.clj |
| CAR_CONTACT_INFO | Flags | Car status (new set) | flags.clj |
| Time in ms | Format | 6 packets + SMALL_RTP | codecs.clj |
| v10 default | Version | Protocol negotiation | packets.clj |

---

## Backward Compatibility

**If supporting v9**:
- Some packets (*) not sent if InSimVer < 10
- Some packets (#) send time in hundredths, not milliseconds
- Need version-aware parsing logic

**If v10-only**:
- No backward compat needed
- All times are milliseconds
- Simpler implementation

**Current Plan**: Assume v10-only (simplest path)

---

## File-by-File Changes

```
src/clj_insim/enum.clj
├─ TINY_HEADER_DATA: position 8 GTH→GTM, add positions 28-30
└─ SMALL_HEADER_DATA: add position 10

src/clj_insim/flags.clj
├─ PLAYER: position 5 reserved→flexible-steer
└─ CAR_CONTACT_INFO: new flag set [blue, yellow, oob, lag]

src/clj_insim/codecs.clj
├─ Verify: IS_OBH, IS_HLV, IS_CON, IS_RIP (time fields)
└─ Add if missing: IS_UCO, IS_CSC, SMALL_RTP

src/clj_insim/packets.clj
└─ Line 62: insim-version 9 → 10

src/clj_insim/parse.clj
├─ Optional: Version tracking context
└─ Optional: Version-aware time parsing

docs/
├─ README.md: v9 → v10
├─ feature_analysis.md: updated feature list
└─ PLAN_TINY_PLH.md: update references
```

---

## Critical Path

1. **Phase 1** ✅ DONE → **Phase 2.1** ✅ DONE → Phase 2.2-2.9 → Phase 3 (sequential)
2. **Phase 4** (can start during Phase 2-3)

```
Phase 1 ✅ (30m) → Phase 2.1 ✅ (1h) → Phase 2.2-2.9 (1h) → Phase 3 (30m) → Done!
                                              ↓
                                           Phase 4 (2-3h, parallel with 3)

COMPLETED: ████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 31% of v10 upgrade
REMAINING: ░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░ 69%
```

---

## Risk Assessment

### Low Risk ✓
- Enum additions (purely additive)
- Flag additions (don't conflict)
- Version update (safe in v10 ecosystem)

### Medium Risk ⚠
- Time field verification (must match reality)
- Codec additions (IS_UCO, IS_CSC if missing)

### Mitigation
- Compare codecs against insim.txt struct definitions
- Run codec tests with sample packets
- Verify with actual LFS connection

---

## Phase 1 Completion Criteria ✅ ALL MET

- [x] All enum positions match insim.txt (verified against SPEC_INSIM_V10.md)
- [x] All flag positions match insim.txt (verified against SPEC_INSIM_V10.md)
- [x] All flag parsing/unparsing works correctly (tested in test suite)
- [x] No breaking changes to API (100% backward compatible)
- [x] Tests pass (33/33 passing, 100 assertions, 0 failures)
- [x] Tests added to regression test suite (automatic v10 compliance checks)
- [x] Documentation updated (PHASE1_FINAL_SUMMARY.md, PHASE1_COMPLETE.md)

## Phases 2-4 Completion Criteria (Pending)

- [ ] All time fields are verified/updated for milliseconds
- [ ] Default version updated to 10
- [ ] Documentation fully updated (README, features, etc)
- [ ] Integration tests with real LFS connection pass

---

## Notes

### Time Formats
- v10: All times in **milliseconds** (unsigned)
- v9: Times in **hundredths of seconds** (unsigned)
- Change affects: OBH, HLV, CON, RIP, UCO, CSC, SMALL_RTP

### Marked Fields
- `(*)` = Not sent if InSimVer < 10
- `(#)` = Different format if InSimVer < 10
- This is LFS behavior, not our code (informational)

### Struct Size Gotchas
- REO: Changed to 48 max players (was 40)
- PLH: Max 48 (new in v10)
- MCI: Still 16 cars (unchanged from v9)
- AXM: Still 60 objects (unchanged from v9)

---

## References

- insim.txt lines 44-65: v10 changelog
- insim.txt: struct definitions for affected packets
- Current code: src/clj_insim/ structure

---

## Execution Plan

### ✅ COMPLETED: Phase 1
**Date**: 2026-07-04  
**Time**: ~30 minutes (implementation + testing)
- Implemented all 5 enum/flag changes
- Added 4 comprehensive test suites (16 assertions)
- All tests passing (33/33, 100 assertions)
- Full backward compatibility maintained

### ✅ COMPLETED: Phase 2.1 - Version-Aware Parse Context
**Date**: 2026-07-04  
**Time**: ~1 hour (implementation + testing)
- Added dynamic version tracking to parse context
- Made read/write functions context-aware with backward compat
- Integrated with client lifecycle
- Added 2 test suites verifying version tracking (105 assertions total)
- All tests passing (35/35)

### 🔄 NEXT: Phase 2.2-2.9 - Time Field Codecs (Estimated: 1-1.5 hours)
1. Verify 6 packet codecs for millisecond times (OBH, HLV, CON, RIP)
2. Add missing IS_UCO, IS_CSC codecs if needed
3. Verify SMALL_RTP time field
4. Implement version-aware time parsing in body parsers

### ⏳ THEN: Phase 3 (Estimated: 30 mins)
1. Update default insim-version from 9 → 10
2. Update all documentation

### 🧪 PARALLEL: Phase 4 (Estimated: 2-3 hours)
1. Write integration tests
2. Test with real LFS connection

**Total Effort Remaining**: ~3.5 hours  
**Overall Progress**: 31% complete (Phase 1 + Phase 2.1 done)
