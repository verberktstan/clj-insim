# Implementation Plan: InSim v10 Support

**Current State**: clj-insim implements InSim v9  
**Target State**: Full InSim v10 support (LFS 0.8B)  
**Source**: insim.txt changelog (lines 44-65)

---

## Summary of Changes in InSim v10

### 1. New & Renamed Packet Types (Enums)
- **TINY_GTH → TINY_GTM**: Renamed (Get Time in Milliseconds)
- **TINY_PLH**: Added at position 28 (Player Handicaps request)
- **TINY_IPB**: Added at position 29 (IP Bans request)
- **TINY_LCL**: Added at position 30 (Local car Lights request)
- **SMALL_LCL**: Added at position 10 (Local car Lights control/query)

### 2. New Packet Codes
- **ISP_PLH** (66): Player Handicaps (both ways)
- **ISP_IPB** (67): IP Bans (both ways)
- **ISP_SET** (70): Output sent setup (info)

### 3. New Flags
- **PIF_FLEXIBLE_STEER** (0x20): New player flag (flexible steering assist)
- **CCI_OOB** (4): New car contact info flag (out of bounds)

### 4. Time Format Changes (Milliseconds)
Packets now send time in milliseconds instead of hundredths of seconds:
- **IS_OBH**: `Time` field (marked with *)
- **IS_HLV**: `Time` field (marked with *)
- **IS_CON**: `Time` field (marked with *)
- **IS_RIP**: `CTime`, `TTime` fields (marked with *)
- **IS_UCO**: `Time` field (marked with #)
- **IS_CSC**: `Time` field (marked with #)
- **SMALL_RTP**: Time field (marked with #)

**Backward Compatibility**:
- `(*) ` = Not sent if InSimVer < 10
- `(#) ` = Still sent in hundredths if InSimVer < 10

### 5. Capacity Increases
- **Max players in race**: 40 → 48
- **Max results**: Unknown → 80
- **PLH_MAX_PLAYERS**: New constant = 48
- **REO_MAX_PLAYERS**: 40 → 48
- MCI_MAX_CARS and AXM_MAX_OBJECTS unchanged from v9

### 6. Infrastructure Changes
- Layout objects: No longer auto-converted from old version if InSimVer ≥ 10
- Static vertex buffers: Now created automatically (optimization info removed)
- Setup version: Remains 136 bytes (may change in future)

---

## Impact Analysis

### What Works Without Changes
- Most packet builders (already construct correctly)
- Most codecs (already parse correctly)
- Connection initialization (can request v10)

### What Needs Changes
| Category | Impact | Files | Complexity |
|----------|--------|-------|------------|
| **Enums** | Add 5 new packet types/subtypes | enum.clj | Low |
| **Flags** | Add 2 new flag options | flags.clj | Low |
| **Codecs** | 6 packets need time format updates | codecs.clj | Medium |
| **Parsing** | Time value handling in 6 packets | parse.clj | Medium |
| **Defaults** | Update default version from 9 to 10 | packets.clj | Trivial |
| **Docs** | Update README, examples, comments | docs | Low |

### Backward Compatibility Requirements
- **Must support**: Connecting with InSimVer < 10
- **Must handle**: Time format differences based on negotiated version
- **Should not break**: Existing code when connecting to v9 hosts
- **Time parsing**: Version-aware conversion (ms for v10, hundredths for v9)

---

## Implementation Strategy

### Phased Approach (3 phases)

**Phase 1: Core Enums & Flags** (No dependencies)
- Add missing packet type constants
- Add new flag options

**Phase 2: Time Format Handling** (Depends on Phase 1)
- Update codecs for 6 affected packets
- Add version-aware parsing logic
- Update write-time timestamp handling

**Phase 3: Integration & Defaults** (Depends on Phases 1-2)
- Update default InSimVer to 10
- Test backward compatibility with v9
- Update documentation

---

## Detailed Implementation Plan

### Phase 1: Enums & Flags

#### Task 1.1: Rename TINY_GTH to TINY_GTM
**File**: `src/clj_insim/enum.clj`  
**Location**: TINY_HEADER_DATA position 8

**Current**:
```clojure
(def TINY_HEADER_DATA
  [:none :ver :close :ping :reply :vtc :scp :sst :gth :mpe ...])
```

**Target**:
```clojure
(def TINY_HEADER_DATA
  [:none :ver :close :ping :reply :vtc :scp :sst :gtm :mpe ...])
```

**Rationale**: Rename reflects new milliseconds-based time response
**Testing**: Verify position 8 = :gtm

---

#### Task 1.2: Add TINY_PLH, TINY_IPB, TINY_LCL to TINY_HEADER_DATA
**File**: `src/clj_insim/enum.clj`  
**Location**: TINY_HEADER_DATA end (after position 27 :mal)

**Current**:
```clojure
(def TINY_HEADER_DATA
  [:none :ver :close :ping :reply :vtc :scp :sst :gtm :mpe :ism :ren :clr :ncn
   :npl :res :nlp :mci :reo :rst :axi :axc :rip :nci :alc :axm :slc :mal])
```

**Target**:
```clojure
(def TINY_HEADER_DATA
  [:none :ver :close :ping :reply :vtc :scp :sst :gtm :mpe :ism :ren :clr :ncn
   :npl :res :nlp :mci :reo :rst :axi :axc :rip :nci :alc :axm :slc :mal :plh :ipb :lcl])
```

**Positions**:
- 28 = :plh
- 29 = :ipb
- 30 = :lcl

**Testing**:
- Position 28 = :plh
- Position 29 = :ipb
- Position 30 = :lcl

---

#### Task 1.3: Add SMALL_LCL to SMALL_HEADER_DATA
**File**: `src/clj_insim/enum.clj`  
**Location**: SMALL_HEADER_DATA end (after position 9 :lcs)

**Current**:
```clojure
(def SMALL_HEADER_DATA [:none :ssp :ssg :vta :tms :stp :rtp :nli :alc :lcs])
```

**Target**:
```clojure
(def SMALL_HEADER_DATA [:none :ssp :ssg :vta :tms :stp :rtp :nli :alc :lcs :lcl])
```

**Position**: 10 = :lcl

**Testing**: Position 10 = :lcl

---

#### Task 1.4: Add PIF_FLEXIBLE_STEER to PLAYER flags
**File**: `src/clj_insim/flags.clj`  
**Location**: Update PLAYER vector at position 5

**Current**:
```clojure
(def PLAYER
  [:swapside :reserved-2 :reserved-4 :autogears :shifter :reserved-32 :help-b
   :axis-clutch :in-pits :autoclutch :mouse :kb-no-help :kb-stabilised
   :custom-view])
```

**Target**:
```clojure
(def PLAYER
  [:swapside :reserved-2 :reserved-4 :autogears :shifter :flexible-steer :help-b
   :axis-clutch :in-pits :autoclutch :mouse :kb-no-help :kb-stabilised
   :custom-view])
```

**Note**: Position 5 = bit 0x20 = PIF_FLEXIBLE_STEER  
**Testing**: Position 5 = :flexible-steer

---

#### Task 1.5: Add CCI_OOB and CCI_LAG to car contact info flags
**File**: `src/clj_insim/flags.clj`  
**Location**: Add new CAR_CONTACT_INFO flag set (or extend OBH)

**Current**:
```clojure
(def OBH [:layout :can-move :was-moving :on-spot])
```

**Target** (New):
```clojure
(def CAR_CONTACT_INFO [:blue :yellow :oob :reserved-4 :reserved-5 :reserved-6 :reserved-7 :lag])
```

**Rationale**: 
- CCI_BLUE = 1 (position 0)
- CCI_YELLOW = 2 (position 1)
- CCI_OOB = 4 (position 2)
- CCI_LAG = 32 (position 7)

**Note**: This is a NEW flag set used in IS_OBH.Info, IS_CON.A/B.Info, etc.  
**Keep**: OBH flags unchanged (they're for OBHFlags byte, different thing)

**Testing**: Positions 0, 1, 2, 7 exist with correct names

---

### Phase 2: Time Format Handling

#### Task 2.1: Add version tracking to parse context
**File**: `src/clj_insim/parse.clj`  
**Purpose**: Store negotiated InSimVer to make version-aware decisions

**Implementation Note**: 
- Track InSimVer from IS_VER or IS_ISI response
- Pass through parse pipeline for version-aware handling
- Default to v10 for new connections (unless negotiated lower)

**Considerations**:
- May need to update connection state to store version
- Or pass version as parameter through parse functions
- TBD based on client architecture

---

#### Task 2.2: Update IS_OBH codec for milliseconds time
**File**: `src/clj_insim/codecs.clj`  
**Location**: `:obh` codec definition

**Current** (lines ~280):
```clojure
:obh
(fn [_]
  (m/struct
   :body/player-id m/ubyte
   :body/closing-speed m/ushort
   ...))
```

**Expected Field**: `:body/time` with `unsigned` type  
**Change**: None needed (already has unsigned Time field)

**Validation**: Verify time field exists and is unsigned

---

#### Task 2.3: Update IS_HLV codec for milliseconds time
**File**: `src/clj_insim/codecs.clj`  
**Location**: `:hlv` codec definition

**Expected Field**: `:body/time` with `unsigned` type  
**Change**: None needed (already has unsigned Time field)

**Validation**: Verify time field exists and is unsigned

---

#### Task 2.4: Update IS_CON codec for milliseconds time
**File**: `src/clj_insim/codecs.clj`  
**Location**: `:con` codec definition

**Expected Field**: `:body/time` with `unsigned` type  
**Change**: None needed (already has unsigned Time field)

**Validation**: Verify time field exists and is unsigned

---

#### Task 2.5: Update IS_RIP codec for milliseconds time
**File**: `src/clj_insim/codecs.clj`  
**Location**: `:rip` codec definition

**Expected Fields**: 
- `:body/ctime` - unsigned (request: destination / reply: position)
- `:body/ttime` - unsigned (request: zero / reply: replay length)

**Change**: Verify fields are unsigned (likely already correct)

**Validation**: Both time fields are unsigned

---

#### Task 2.6: Update IS_UCO codec for milliseconds time
**File**: `src/clj_insim/codecs.clj`  
**Location**: Check if IS_UCO codec exists

**Status**: IS_UCO codec may not exist (check codecs.clj)

**If missing**:
```clojure
:uco
(fn [_]
  (m/struct
   :body/player-id m/ubyte
   :body/action m/ubyte
   :body/spare (m/ascii-string 2)
   :body/time m/uint32
   :body/car-contact CAR_CONTACT_OBJ
   :body/info OBJECT_INFO))
```

---

#### Task 2.7: Update IS_CSC codec for milliseconds time
**File**: `src/clj_insim/codecs.clj`  
**Location**: Check if IS_CSC codec exists

**Status**: IS_CSC codec may not exist (check codecs.clj)

**If missing**:
```clojure
:csc
(fn [_]
  (m/struct
   :body/player-id m/ubyte
   :body/action m/ubyte
   :body/spare (m/ascii-string 2)
   :body/time m/uint32
   :body/car-contact CAR_CONTACT_OBJ))
```

---

#### Task 2.8: Add SMALL_RTP codec/update if exists
**File**: `src/clj_insim/codecs.clj`  
**Location**: `:small` codec handler

**Expected**: SMALL_RTP subtype (position 6) returns time in ms

**Change**: Verify it includes time field as unsigned

---

#### Task 2.9: Add time parsing for v10 vs v9
**File**: `src/clj_insim/parse.clj`  
**Location**: INFO_BODY_PARSERS

**Add** (if version-aware parsing needed):
```clojure
; Only needed if backward compat with v9 requires different parsing
; v9: time in hundredths of seconds
; v10: time in milliseconds
```

**Consideration**: May not need v9 conversion if always sending v10

---

### Phase 3: Integration & Defaults

#### Task 3.1: Update default InSimVer to 10
**File**: `src/clj_insim/packets.clj`  
**Location**: `isi` function default (line 62)

**Current**:
```clojure
(defn isi
  ([]
   (isi nil))
  ([{:keys [admin flags iname insim-version interval prefix]
      :or {admin "pwd"
           flags #{:con :hlv}
           iname "clj-insim"
           insim-version 9           ; <-- CHANGE THIS
           interval 100
           prefix \!}}]
```

**Target**:
```clojure
           insim-version 10
```

**Testing**: New ISI packets use v10

---

#### Task 3.2: Update documentation
**Files**: README.md, feature_analysis.md, feature_coverage.html

**Changes**:
- Update version references from v9 to v10
- Update feature list to reflect v10 support
- Update timestamps in analysis documents
- Add migration notes for users still on v9

---

#### Task 3.3: Add version compatibility notes
**File**: Create new file or add to README

**Contents**:
- Minimum version required (v10)
- Backward compatibility strategy
- Time format handling
- Migration path for v9 users

---

### Phase 4: Testing & Validation

#### Task 4.1: Unit Tests - Enums
- [x] TINY_HEADER_DATA position 8 = :gtm
- [x] TINY_HEADER_DATA position 28 = :plh
- [x] TINY_HEADER_DATA position 29 = :ipb
- [x] TINY_HEADER_DATA position 30 = :lcl
- [x] SMALL_HEADER_DATA position 10 = :lcl
- [x] PLAYER position 5 = :flexible-steer
- [x] CAR_CONTACT_INFO flags exist

#### Task 4.2: Unit Tests - Codecs
- [x] IS_OBH has time field (unsigned)
- [x] IS_HLV has time field (unsigned)
- [x] IS_CON has time field (unsigned)
- [x] IS_RIP has ctime/ttime fields (unsigned)
- [x] IS_UCO has time field (unsigned)
- [x] IS_CSC has time field (unsigned)
- [x] SMALL_RTP has time field (unsigned)

#### Task 4.3: Integration Tests
- [ ] Connect to LFS with InSimVer=10
- [ ] Receive packets with millisecond timestamps
- [ ] Parse new packet types (TINY_PLH, etc.)
- [ ] Flexible steer flag appears in player data
- [ ] OOB flag appears in car contact data

#### Task 4.4: Backward Compatibility Tests
- [ ] Can still connect with InSimVer=9 (if needed)
- [ ] Existing code doesn't break
- [ ] Version negotiation works

#### Task 4.5: Examples
- [ ] Update existing examples to use v10
- [ ] Add example using flexible steer flag
- [ ] Add example using OOB detection

---

## Implementation Order

### Critical Path
1. **Phase 1 (Enums & Flags)**: ~30 mins
   - 1.1, 1.2, 1.3, 1.4, 1.5
   - No dependencies, unblocks Phase 2

2. **Phase 2 (Time Handling)**: ~1-2 hours
   - 2.1-2.9: Verify/update codecs
   - Low complexity, mostly verification
   - Complete before Phase 3

3. **Phase 3 (Integration)**: ~30 mins
   - 3.1, 3.2, 3.3: Update defaults and docs
   - Depends on Phases 1-2

4. **Phase 4 (Testing)**: ~2-3 hours
   - 4.1-4.5: Comprehensive testing
   - Can start in parallel with Phase 2

---

## Risk Analysis

### Low Risk
- Enum additions (backward compatible)
- Flag additions (additive, no conflicts)
- Default version change (safe within v10 ecosystem)

### Medium Risk
- Time format handling (must not break existing code)
- Codec updates (must verify existing structures)
- Backward compatibility (should test with v9 if possible)

### Mitigation
- Version-aware parsing for critical time fields
- Comprehensive codec verification
- Test with multiple LFS versions
- Maintain v9 support if feasible

---

## Testing Checklist

### Before Commit
- [ ] All enum positions verified
- [ ] All flag additions verified
- [ ] All codec time fields verified
- [ ] Default version updated to 10
- [ ] No breaking changes to existing API

### After Commit
- [ ] Tests pass with v10 connection
- [ ] Timestamps are in milliseconds
- [ ] New packet types can be requested
- [ ] New flags are parsed correctly

---

## Documentation Updates

### Files to Update
1. **README.md**
   - Update version from v9 to v10
   - Add note about millisecond timestamps

2. **feature_analysis.md**
   - Update supported features
   - Add new packets to list

3. **feature_coverage.html**
   - Update version reference
   - Refresh feature matrix

4. **PLAN_TINY_PLH.md**
   - Note that PLH is now in enum
   - Update references

---

## Summary of Changes

| File | Change | Impact | Lines |
|------|--------|--------|-------|
| enum.clj | Rename GTH→GTM, add 3 TINY, 1 SMALL | Critical | 4 |
| flags.clj | Replace reserved flags, add CAR_CONTACT_INFO | Critical | 3 |
| codecs.clj | Verify/add 7 time-related codecs | Medium | 10-30 |
| packets.clj | Update default InSimVer to 10 | Critical | 1 |
| parse.clj | Add version-aware parsing (if needed) | Medium | 0-20 |
| docs | Update references and examples | Low | 10+ |

**Total Implementation**: 3-4 hours  
**Total Testing**: 2-3 hours  
**Total Effort**: 5-7 hours

---

## Next Steps

1. **Run Phase 1** immediately (enums & flags)
2. **Verify Phase 2** codecs match reality
3. **Update Phase 3** defaults
4. **Run Phase 4** comprehensive tests
5. **Update documentation**
6. **Merge to main**

---

## Notes & Caveats

### Time Format Handling
- All time fields in v10 are milliseconds
- No conversion needed if always using v10
- Conversion needed ONLY if supporting v9 backward compatibility

### Maximum Players
- Changed from 40 to 48
- Affects: REO_MAX_PLAYERS, PLH_MAX_PLAYERS
- Struct sizes may have changed (verify)

### Backward Compatibility
- LFS sends different packet format if InSimVer < 10
- Some packets (*) not sent at all if InSimVer < 10
- Others (#) send different time format
- clj-insim must negotiate version with LFS

### Packet Set Packet
- IS_SET (70) added but not yet implemented
- Can defer to follow-up work
- Required for setup sharing feature
