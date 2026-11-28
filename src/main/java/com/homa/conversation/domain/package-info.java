/**
 * Conversation's domain: the chats between a customer and a provider (CHT-1 to CHT-5). It imports only
 * {@code java.*}; the kernel isn't needed yet.
 *
 * <p>Built (slice 8):
 * <ul>
 *   <li>{@link com.homa.conversation.domain.Conversation}: an Enquiry a customer starts (CHT-1, CUS-5) or the chat of
 *       one reservation, and its messages (CHT-2).</li>
 *   <li>{@link com.homa.conversation.domain.ReservationChatWindow}: the replica of a reservation's status and
 *       Protection Window end, from which a reservation's chat is open or read-only at a given moment (CHT-2,
 *       CHT-5), and {@link com.homa.conversation.domain.ChatAccess}, its answer. No scheduler and no stored "locked"
 *       flag.</li>
 *   <li>{@link com.homa.conversation.domain.CustomerStanding}: the replica of who is registered, verified and
 *       suspended, so only a customer in good standing starts an Enquiry (CHT-1, CUS-5). Its suspension is a period,
 *       so a 7- or 30-day suspension ends by itself (CUS-6).</li>
 * </ul>
 *
 * <p>Not built, on purpose:
 * <ul>
 *   <li><b>The off-platform reminder</b> (CHT-3) is copy every chat shows in the apps. No domain object holds it, and
 *       messages are kept as written.</li>
 *   <li><b>Support reading a chat as evidence</b> (CHT-4, slice 5) is a read, not a change to a conversation: it
 *       plugs in beside the party's view as a use case for the Support role.</li>
 *   <li><b>Suspension events</b> (CUS-4, CUS-6): {@code CustomerStanding.suspended} and {@code reactivated} are ready
 *       for {@code CustomerSuspendedIE} and {@code CustomerReactivatedIE}, which Customer publishes from slice 6.</li>
 *   <li><b>Retention</b> (DAT-5, slice 9): a {@code ChatRetention} type, kept 12 months after the Protection Window
 *       end for a reservation's chat and after the last message for an Enquiry. No Homa-set value lives here yet.</li>
 *   <li><b>Provider's staff</b> (PRV-11, slice 4): who may speak for a provider is decided in the use cases, which
 *       today know the owner only.</li>
 * </ul>
 */
package com.homa.conversation.domain;
