# Pilot Report v0.2 - Multi-domain stress test

## Scope

Expanded master pilot: 118 rows total.

Source types:
- PREFERRED: 59
- INCLUDE: 24
- EXCLUDE: 5
- INDEX: 30

Mapping levels:
- L1_EXACT: 98
- L2_CM_NARROWER: 2
- L3_CM_BROADER: 9
- L4_RELATED: 4
- L7_EXCLUDED_RELATION: 5

New pilot batches:
- Complex infectious manifestations: 18 rows
- Neoplasm: 15 rows
- Injury: 14 rows
- Obstetrics: 12 rows
- Chapter XXI / Z codes: 12 rows

## Structural findings

### 1. Mapping level and CM expansion dimensions are different things

ICD-10-CM frequently adds dimensions that WHO does not encode in a source term.

Examples:
- Injury: laterality, displacement, open/closed, encounter, healing, sequela
- Obstetrics: trimester, pregnancy/childbirth/puerperium, control method, term status, fetus number, chorionicity, amnionicity
- Neoplasm: sex, laterality, subsite
- Z codes: trimester or delivery type below an otherwise equivalent parent concept

These dimensions must be stored separately from semantic relationship.

### 2. Do not force the deepest CM code

WHO S52.5 Fracture of lower end of radius maps semantically to CM S525 Fracture of lower end of radius.

CM descendants then specify named fracture, laterality, encounter and healing.

The existence of descendants does not make S525 an invalid mapping target.

### 3. Injury validates the parent-target rule

WHO Index:
- Colles' fracture -> S52.5

CM:
- S5253 Colles' fracture
- then laterality + encounter + healing descendants

The source term itself justifies S5253, but not right/left or 7th-character detail.

### 4. Obstetric mappings require explicit context discipline

WHO O24.4 Diabetes mellitus arising in pregnancy maps to CM O244 Gestational diabetes mellitus.

CM children then split:
- pregnancy
- childbirth
- puerperium
- diet controlled
- insulin controlled
- oral hypoglycemic drugs
- unspecified control

The mapper must not invent these attributes.

### 5. Same code lineage can still have semantic drift

WHO O80:
- Single spontaneous delivery

ICD-10-CM O80:
- Encounter for full-term uncomplicated delivery

The code lineage is the same, but the defining semantic axis changed.
The pilot marks this L4_RELATED for mandatory review rather than mechanically L1_EXACT.

### 6. Z codes require a broader semantic model than disease terminology

Examples include:
- screening
- pregnancy supervision
- outcome of delivery
- liveborn status
- personal history

The target should be called target concept/term, not disease term.

### 7. Neoplasm exposes information-loss mappings

WHO Index may preserve histology such as infiltrating duct or lobular adenocarcinoma while the target CM site code does not.

A shared code family does not make such a mapping exact.
The histology loss must be reflected as L3_CM_BROADER or L4_RELATED.

### 8. Infectious disease shows CM can absorb WHO manifestation detail

WHO A39.5 Meningococcal heart disease contains manifestations such as:
- endocarditis
- myocarditis
- pericarditis

ICD-10-CM explicitly creates:
- A3951
- A3952
- A3953

This supports term-level mapping of WHO inclusion expressions.

## Prompt changes required

Master Prompt v0.2 now requires:
1. reconstruct source expression first
2. classify semantic kind
3. generate bounded candidates
4. choose most semantically equivalent target, not deepest target
5. assign mapping level independently
6. record CM expansion dimensions independently
7. apply domain-specific safeguards
8. force review for semantic drift and missing required context

## Next gold-set gaps

The 118-row pilot now contains L4 examples, but still lacks validated production examples for:
- L5_MULTIPLE
- L6_NO_MATCH

Do not manufacture these classes just to balance the dataset.
Add them only when real source expressions genuinely require them.
