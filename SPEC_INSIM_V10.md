# InSim Protocol v10 Specification

**Version**: 10  
**Released**: LFS 0.8B  
**Source**: insim.txt (lines 44-65)  
**Previous Version**: v9 (LFS 0.7A)

---

## Complete Changelog v9 → v10

### Protocol Version
```c
const int INSIM_VERSION = 10;  // (was 9 in v9)
```

### 1. New Packet Type: TINY_GTM (renamed from TINY_GTH)

**Specification**:
```c
enum // TINY subtypes (excerpt)
{
  // ...
  TINY_GTM,  //  8 - info request  : get time in ms (in SMALL_RTP)
  // (was: TINY_GTH)
  // ...
}
```

**Purpose**: Request current race time in milliseconds  
**Response**: SMALL_RTP packet with time in milliseconds  
**Change**: Renamed to indicate milliseconds format

---

### 2. New Packet Types (Handicaps & IP Bans)

#### IS_PLH / TINY_PLH
```c
const int PLH_MAX_PLAYERS = 48;

enum
{
  ISP_PLH,    // 66 - both ways    : set player handicaps
}

enum
{
  TINY_PLH,   // 28 - info request : send IS_PLH listing player handicaps
}
```

**Details**: See SPEC_TINY_PLH.md for complete documentation

#### IS_IPB / TINY_IPB
```c
const int IPB_MAX_BANS = 120;

enum
{
  ISP_IPB,    // 67 - both ways    : set IP bans
}

enum
{
  TINY_IPB,   // 29 - info request : send IS_IPB listing IP bans
}
```

**Purpose**: Manage IP ban list on host  
**Max Entries**: 120  
**Status**: New in v0.7F

---

### 3. New Packet Type: SMALL_LCL (Car Lights Control)

```c
enum // SMALL subtypes
{
  SMALL_LCL,  // 10 - both ways    : set or get local car lights
}

enum // TINY subtypes
{
  TINY_LCL,   // 30 - info request : send a SMALL_LCL for local car lights
}
```

**Purpose**: Full control of car lights including fog and extra lights  
**Previous**: SMALL_LCS for basic switches (still available)  
**Change**: New comprehensive lights control added  
**Status**: Partially implemented in v0.7E, expanded in v10

---

### 4. New Player Flag: PIF_FLEXIBLE_STEER

```c
enum // Player info flags
{
  PIF_LEFTSIDE      = 1,
  PIF_RESERVED_2    = 2,
  PIF_RESERVED_4    = 4,
  PIF_AUTOGEARS     = 8,
  PIF_SHIFTER       = 0x10,
  PIF_FLEXIBLE_STEER = 0x20,    // NEW in v10
  PIF_HELP_B        = 0x40,
  PIF_AXIS_CLUTCH   = 0x80,
  // ... (more flags follow)
}
```

**Meaning**: Player has flexible steering assist enabled  
**Location**: Bit 5 (0x20) in player flags  
**Scope**: Per-player input mode preference  
**Status**: New in v10 (LFS 0.8B)

---

### 5. New Car Contact Flag: CCI_OOB

```c
#define CCI_BLUE        1       // car is in the way of lap-ahead driver
#define CCI_YELLOW      2       // car is slow/stopped in dangerous place
#define CCI_OOB         4       // NEW in v10: car is outside the path
#define CCI_LAG         32      // car is lagging (was in v0.5Z)

// Info byte format: [bit0: blue] [bit1: yellow] [bit2: oob] [bit7: lag]
```

**Location**: Bit 2 (0x04) in car contact info byte  
**Affected Packets**:
- IS_OBH - CarContOBJ.Info byte
- IS_CON - CarContact.Info byte (both cars A and B)
- IS_HLV - CarContOBJ.Info byte

**Meaning**: Indicates if car is currently outside the valid track path  
**Status**: New in v10 (LFS 0.8B)

---

### 6. Time Format Changes (Milliseconds)

**In v10**: All time fields are in **milliseconds**  
**In v9**: Time fields were in **hundredths of seconds**

#### Affected Packets

**Marked `(*)` - Not sent if InSimVer < 10**:
- IS_OBH : Time field (unsigned ms)
- IS_HLV : Time field (unsigned ms)
- IS_CON : Time field (unsigned ms)
- IS_RIP : CTime, TTime fields (unsigned ms)

**Marked `(#)` - Sent in different format if InSimVer < 10**:
- IS_UCO : Time field (unsigned ms in v10, hundredths in v9)
- IS_CSC : Time field (unsigned ms in v10, hundredths in v9)
- SMALL_RTP : Time field (unsigned ms in v10, hundredths in v9)

#### Conversion Examples
```
v10: 1000 ms = 1 second
v9:   100 (hundredths) = 1 second

To convert v9 to v10: value_v10 = value_v9 * 10
To convert v10 to v9: value_v9 = value_v10 / 10
```

---

### 7. Capacity Increases

#### Maximum Players in Race
```
v9:  40 maximum
v10: 48 maximum
```

**Affected Constants**:
```c
const int PLH_MAX_PLAYERS = 48;  // (new, didn't exist in v9)
const int REO_MAX_PLAYERS = 48;  // (was 40)
```

**Note**: MCI_MAX_CARS and AXM_MAX_OBJECTS unchanged from v9

#### Maximum Results
```
v9:  Unknown/unspecified
v10: 80 maximum
```

---

### 8. Detailed Packet Structure Changes

#### IS_OBH (Object Hit)
```c
// Size: 28 bytes (same)
// NEW: Time field is in milliseconds
struct IS_OBH
{
  byte    Size;      // 28
  byte    Type;      // ISP_OBH
  byte    ReqI;      // 0
  byte    PLID;
  
  word    SpClose;
  word    SpW;
  
  unsigned Time;     // NEW: in milliseconds (was hundredths in v9)
  
  CarContOBJ C;      // Contains Info byte with NEW CCI_OOB flag
  
  short   X;
  short   Y;
  
  byte    Zbyte;
  byte    Sp1;
  byte    Index;
  byte    OBHFlags;
};
```

#### IS_HLV (Hot Lap Validity)
```c
// Size: 20 bytes (same)
// NEW: Time field is in milliseconds
struct IS_HLV
{
  byte    Size;      // 20
  byte    Type;      // ISP_HLV
  byte    ReqI;      // 0
  byte    PLID;
  
  byte    HLVC;
  byte    Sp1;
  word    SpW;
  
  unsigned Time;     // NEW: in milliseconds
  
  CarContOBJ C;      // Contains Info byte with NEW CCI_OOB flag
};
```

#### IS_CON (Contact)
```c
// Size: 44 bytes (same)
// NEW: Time field is in milliseconds
struct IS_CON
{
  byte    Size;      // 44
  byte    Type;      // ISP_CON
  byte    ReqI;      // 0
  byte    Zero;
  
  word    SpClose;
  word    SpW;
  
  unsigned Time;     // NEW: in milliseconds
  
  CarContact A;      // Contains Info byte with NEW CCI_OOB flag
  CarContact B;      // Contains Info byte with NEW CCI_OOB flag
};
```

#### IS_RIP (Replay Information)
```c
// Size: 80 bytes (same)
// NEW: Time fields in milliseconds
struct IS_RIP
{
  byte    Size;      // 80
  byte    Type;      // ISP_RIP
  byte    ReqI;      // request: non-zero / reply: same value
  byte    Error;
  
  byte    MPR;
  byte    Paused;
  byte    Options;
  byte    Sp3;
  
  unsigned CTime;    // NEW: in milliseconds (was hundredths)
  unsigned TTime;    // NEW: in milliseconds (was hundredths)
  
  char    RName[64];
};
```

#### IS_UCO (User Control Object) - Checkpoint/Circle
```c
// NEW: Time field in milliseconds
struct IS_UCO
{
  byte    Size;      // 28
  byte    Type;      // ISP_UCO
  byte    ReqI;      // 0
  byte    PLID;
  
  byte    Sp0;
  byte    UCOAction;
  byte    Sp2;
  byte    Sp3;
  
  unsigned Time;     // in milliseconds (hundredths in v9 if v9 compat)
  
  CarContOBJ C;
  ObjectInfo Info;
};
```

#### IS_CSC (Car State Changed)
```c
// NEW: Time field in milliseconds
struct IS_CSC
{
  byte    Size;      // 20
  byte    Type;      // ISP_CSC
  byte    ReqI;      // 0
  byte    PLID;
  
  byte    Sp0;
  byte    CSCAction;
  byte    Sp2;
  byte    Sp3;
  
  unsigned Time;     // in milliseconds (hundredths in v9 if v9 compat)
  
  CarContOBJ C;
};
```

#### SMALL_RTP (Race Time Packet)
```c
// SMALL packet with subtype 6
// NEW: Time in milliseconds
struct reply
{
  byte    Size;      // 8
  byte    Type;      // ISP_SMALL
  byte    ReqI;      // echo of request
  byte    SubT;      // SMALL_RTP (6)
  
  unsigned Time;     // in milliseconds (hundredths in v9 if v9 compat)
};
```

---

### 9. Backward Compatibility Notes

#### Layout Objects
```
v9:  Old version objects were auto-converted to new format
v10: Objects no longer auto-converted if InSimVer >= 10
     Reason: Version checking allows proper handling
```

#### Static Vertex Buffers
```
v9:  Required documentation about vertex buffer optimization
v10: Static vertex buffers now created automatically
     Info about 'optimisation' removed from documentation
```

#### Setup Version
```
Size: 136 bytes (unchanged)
Note: May change in future LFS versions
```

---

### 10. Complete Enum Changes

#### TINY_HEADER_DATA

**v9**:
```c
enum
{
  TINY_NONE,    //  0
  TINY_VER,     //  1
  // ... (0-27 total items)
  TINY_MAL,     // 27 - Mods Allowed
  // END at position 27
}
```

**v10**:
```c
enum
{
  TINY_NONE,    //  0
  TINY_VER,     //  1
  // ...
  TINY_MAL,     // 27 - Mods Allowed
  TINY_PLH,     // 28 - Player Handicaps (NEW)
  TINY_IPB,     // 29 - IP Bans (NEW)
  TINY_LCL,     // 30 - Local Car Lights (NEW)
  // END at position 30
}
```

#### SMALL_HEADER_DATA

**v9**:
```c
enum
{
  SMALL_NONE,   //  0
  // ...
  SMALL_LCS,    //  9 - Local Car Switches
  // END at position 9
}
```

**v10**:
```c
enum
{
  SMALL_NONE,   //  0
  // ...
  SMALL_LCS,    //  9 - Local Car Switches
  SMALL_LCL,    // 10 - Local Car Lights (NEW, replaces/extends LCS)
  // END at position 10
}
```

---

## Summary Table

| Item | v9 | v10 | Impact |
|------|----|----|--------|
| Version Code | 9 | 10 | Protocol version |
| TINY_GTH | Position 8 | Renamed to TINY_GTM | Timestamp format |
| TINY_PLH | Missing | Position 28 | New handicap request |
| TINY_IPB | Missing | Position 29 | New ban request |
| TINY_LCL | Missing | Position 30 | New lights request |
| SMALL_LCL | Missing | Position 10 | New lights control |
| Time format | Hundredths | Milliseconds | 6 packets + SMALL_RTP |
| Max players | 40 | 48 | Affects REO, PLH |
| CCI_OOB flag | Missing | Bit 2 | New car status |
| PIF_FLEXIBLE_STEER | Missing | Bit 5 | New player mode |
| Layout conv | Auto | Conditional | Depends on version |

---

## Implementation Notes

### For clj-insim Developers

1. **Enums**: Update TINY_HEADER_DATA and SMALL_HEADER_DATA
2. **Flags**: Add flexible-steer to PLAYER, add CAR_CONTACT_INFO flags
3. **Codecs**: Verify time fields in 6 packets are unsigned
4. **Parsing**: No conversion needed if always v10 (times are already ms)
5. **Default**: Change default insim-version from 9 to 10

### Breaking Changes
- None if only supporting v10
- Time format differs if supporting v9 (requires version-aware parsing)

### Non-Breaking Additions
- New enums (additive)
- New flags (additive, don't conflict)
- New capacity (larger arrays, compatible)

---

## References

**InSim.txt sections**:
- Lines 44-65: Version 10 changelog
- Line 19: INSIM_VERSION constant
- Various packet definitions (see individual specs)

**Related documentation**:
- PLAN_INSIM_V10.md: Implementation plan
- PLAN_INSIM_V10_QUICK.md: Quick checklist
- SPEC_TINY_PLH.md: Player handicaps specification
