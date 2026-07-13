# Event-log and tombstone growth

## Safety invariant

Ncy must never discard an exact chat or message deletion identifier while an old
backup can be paired again and contribute history. Time elapsed and acknowledgements
from currently paired devices are not sufficient: a newly paired stale restore is
outside that acknowledgement set and could reintroduce the deleted object.

Consequently, automatic retention may delete superseded content and media, but it
must retain an exact deletion record. Approximate filters are not acceptable because
false negatives resurrect data and false positives hide legitimate new data.

## Implementation stages

- [x] **Lock down compaction semantics.** Android and Core regression tests prove a
  chat tombstone remains dominant even when later-HLC stale history arrives. Core
  now retains unknown future operations verbatim instead of deleting them during
  compaction.
- [x] **Introduce a compact graveyard ledger.** Android archives deletions per profile;
  Core archives them beside the event store. Old tombstone-only chat storage is
  removed, catch-up emits the original event, and inbound stale history is rejected
  through an indexed exact chat-ID set.
- [x] **Make graveyards portable.** Android and Desktop full backups export/import
  graveyards, preserve profile placement, validate clocks, and refuse stale chat
  directories covered by an imported deletion.
- [x] **Offer explicit dangerous purge.** Android and Desktop settings require a
  strong resurrection warning; Server requires `purge-tombstones --yes-i-understand`.
  Purge is never automatic and reports the number of exact records forgotten.
- [ ] **Compact active-chat message tombstones.** Store exact message deletion records
  separately from bulky event envelopes while reconstructing normal wire tombstones
  for catch-up.
- [ ] **Add retention observability.** Report event, tombstone, graveyard, and reclaimed
  byte counts; warn before compaction cannot materially reduce a very large live chat.
- [ ] **Define safe history generations.** A future generation reset can revoke all
  old pairings and reject old-generation events before purging. Until then, manual
  purge deliberately accepts the clearly stated stale-resurrection risk.

No stage may use age alone to delete tombstones.
