const API = "http://localhost:8081/api";
const GLYPHS = { Music: "♫", Arts: "✿", Food: "♨", Community: "♡", Technology: "⌘", Wellness: "✳" };
const escapeHtml = value => String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
const currentUser = () => {
  try { return JSON.parse(localStorage.getItem("eventhub_user") || "null"); } catch { return null; }
};
const authHeaders = () => {
  const token = localStorage.getItem("eventhub_token");
  return token ? { Authorization: `Bearer ${token}` } : {};
};
async function api(path, options = {}) {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: { ...authHeaders(), ...(options.body ? { "Content-Type": "application/json" } : {}), ...options.headers }
  });
  const text = await response.text();
  let payload = null;
  if (text) {
    try { payload = JSON.parse(text); } catch { throw new Error("The server returned an unreadable response"); }
  }
  if (!response.ok) throw new Error(payload?.error || payload?.message || `Request failed (${response.status})`);
  return payload;
}
const send = (method, body) => ({ method, body: JSON.stringify(body) });
const bookingsUrl = bookingReference => `bookings.html?booking=${encodeURIComponent(bookingReference)}`;
const money = amount => Number(amount) === 0 ? "Free" : new Intl.NumberFormat("en-IN", {
  style: "currency", currency: "INR", maximumFractionDigits: 2
}).format(Number(amount));
const dateLabel = (date, time) => {
  const value = new Date(`${date}T${time || "12:00"}`);
  return Number.isNaN(value.getTime()) ? escapeHtml(date) : value.toLocaleString(undefined, { weekday: "short", month: "short", day: "numeric", year: "numeric", ...(time ? { hour: "numeric", minute: "2-digit" } : {}) });
};
function showToast(message, isError = false) {
  document.querySelector(".toast")?.remove();
  const toast = document.createElement("div");
  toast.className = `toast${isError ? " error" : ""}`;
  toast.setAttribute("role", isError ? "alert" : "status");
  toast.textContent = message;
  document.body.append(toast);
  window.setTimeout(() => toast.remove(), 4200);
}
function setMessage(form, message, error = false) {
  const node = form.querySelector(".form-message");
  if (node) { node.textContent = message; node.classList.toggle("error", error); }
}
function renderHeader() {
  const header = document.getElementById("site-header");
  if (!header) return;
  const user = currentUser();
  const pathname = location.pathname.split("/").pop();
  const links = [
    ["index.html", "Home"], ["events.html", "Explore"], ...(user ? [["bookings.html", "My bookings"]] : []),
    ...(user?.role === "ORGANIZER" || user?.role === "ADMIN" ? [["dashboard.html", "Organizer"]] : []),
    ...(user?.role === "ADMIN" ? [["admin.html", "Admin"]] : [])
  ];
  header.className = "site-header";
  header.innerHTML = `<a class="brand" href="index.html"><span class="brand-mark">e</span> eventhub</a>
    <button class="menu-toggle" aria-label="Toggle navigation" aria-expanded="false">☰</button>
    <nav class="nav-links" aria-label="Main navigation">${links.map(([href, label]) => `<a class="${pathname === href ? "active" : ""}" href="${href}">${label}</a>`).join("")}</nav>
    <div class="nav-actions">${user ? `<a class="nav-login" href="profile.html">${escapeHtml(user.fullName)}</a><button class="small-button" id="logout-button">Log out</button>` : `<a class="nav-login" href="login.html">Log in</a><a class="nav-register" href="register.html">Join EventHub</a>`}</div>`;
  header.querySelector(".menu-toggle")?.addEventListener("click", event => {
    const button = event.currentTarget;
    const nav = header.querySelector(".nav-links");
    const open = nav.classList.toggle("open");
    button.setAttribute("aria-expanded", String(open));
  });
  header.querySelector("#logout-button")?.addEventListener("click", () => {
    localStorage.removeItem("eventhub_token");
    localStorage.removeItem("eventhub_user");
    location.href = "index.html";
  });
}
function renderFooter() {
  const footer = document.getElementById("site-footer");
  if (footer) footer.className = "site-footer", footer.innerHTML = `<a class="footer-brand" href="index.html">eventhub</a><span>Find your people. Make a memory.</span><span>© ${new Date().getFullYear()} EventHub</span>`;
}
function eventCard(event) {
  const image = event.imageUrl ? `<img src="${escapeHtml(event.imageUrl)}" alt="" loading="lazy" onerror="this.remove()">` : "";
  return `<article class="event-card"><a href="event.html?id=${encodeURIComponent(event.id)}" aria-label="View ${escapeHtml(event.name)}">
    <div class="event-image" data-category="${escapeHtml(event.category)}">${image}<span class="event-glyph" aria-hidden="true">${GLYPHS[event.category] || "✦"}</span><span class="event-category">${escapeHtml(event.category)}</span></div></a>
    <div class="event-content"><h3><a href="event.html?id=${encodeURIComponent(event.id)}">${escapeHtml(event.name)}</a></h3>
    <div class="event-meta"><span>▦ &nbsp;${dateLabel(event.eventDate, event.startTime)}</span><span>⌖ &nbsp;${escapeHtml(event.venue)}</span></div>
    <div class="event-bottom"><span class="price">${money(event.ticketPrice)} <small>/ ticket</small></span><span class="seats">${event.availableTickets} left</span></div></div></article>`;
}
async function loadHome() {
  const target = document.getElementById("featured-events");
  const events = await api("/events");
  target.innerHTML = events.length ? events.slice(0, 3).map(eventCard).join("") : `<div class="empty-state">No upcoming events just yet. Check back soon!</div>`;
}
async function loadEvents() {
  const target = document.getElementById("event-results");
  const search = document.getElementById("event-search");
  const category = document.getElementById("event-category");
  const date = document.getElementById("event-date");
  const sort = document.getElementById("event-sort");
  const params = new URLSearchParams(location.search);
  search.value = params.get("search") || "";
  category.value = params.get("category") || "";
  async function refresh() {
    target.innerHTML = `<div class="loading">Finding events…</div>`;
    try {
      const query = new URLSearchParams();
      if (search.value.trim()) query.set("search", search.value.trim());
      if (category.value) query.set("category", category.value);
      let events = await api(`/events${query.size ? `?${query}` : ""}`);
      if (date.value) events = events.filter(event => event.eventDate >= date.value);
      if (sort.value === "price-asc") events.sort((a, b) => Number(a.ticketPrice) - Number(b.ticketPrice));
      if (sort.value === "price-desc") events.sort((a, b) => Number(b.ticketPrice) - Number(a.ticketPrice));
      target.innerHTML = events.length ? events.map(eventCard).join("") : `<div class="empty-state">No events match those filters. Try a different search.</div>`;
    } catch (error) { target.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}. Is the EventHub API running?</div>`; }
  }
  let timer;
  search.addEventListener("input", () => { clearTimeout(timer); timer = setTimeout(refresh, 250); });
  [category, date, sort].forEach(control => control.addEventListener("change", refresh));
  await refresh();
}
async function requireAuth() {
  if (!localStorage.getItem("eventhub_token")) {
    location.href = `login.html?return=${encodeURIComponent(location.href)}`;
    return null;
  }
  try { return await api("/auth/me"); }
  catch (error) {
    localStorage.removeItem("eventhub_token");
    localStorage.removeItem("eventhub_user");
    showToast(error.message, true);
    location.href = "login.html";
    return null;
  }
}
async function loadEvent() {
  const host = document.getElementById("event-detail");
  const id = new URLSearchParams(location.search).get("id");
  if (!id || !/^\d+$/.test(id)) { host.innerHTML = `<div class="empty-state">That event could not be found. <a href="events.html">Explore events</a></div>`; return; }
  const event = await api(`/events/${id}`);
  document.title = `${event.name} — EventHub`;
  const image = event.imageUrl ? `<img src="${escapeHtml(event.imageUrl)}" alt="${escapeHtml(event.name)}" onerror="this.remove()">` : `<span class="event-glyph" aria-hidden="true">${GLYPHS[event.category] || "✦"}</span>`;
  host.innerHTML = `<div class="event-detail-layout"><section><div class="event-cover event-image" data-category="${escapeHtml(event.category)}">${image}</div>
    <div class="event-detail-copy"><span class="eyebrow">${escapeHtml(event.category)} · HOSTED BY ${escapeHtml(event.organizerName)}</span><h1>${escapeHtml(event.name)}</h1><div class="detail-facts">
      <div class="detail-fact"><small>When</small>${dateLabel(event.eventDate, event.startTime)}</div><div class="detail-fact"><small>Where</small>${escapeHtml(event.venue)}</div>
      <div class="detail-fact"><small>Tickets</small>${event.availableTickets} available</div><div class="detail-fact"><small>Organizer</small>${escapeHtml(event.organizerName)}</div></div>
      <h2>About this event</h2><p>${escapeHtml(event.description).replace(/\n/g, "<br>")}</p></div></section>
      <aside class="booking-box"><h3>Save your spot</h3><div class="big-price">${money(event.ticketPrice)} <small>/ ticket</small></div>
      <label>Number of tickets <input id="ticket-quantity" type="number" min="1" max="${Math.min(20, event.availableTickets)}" value="1" ${event.availableTickets < 1 ? "disabled" : ""}></label>
      <p class="ticket-total">Total: <strong id="ticket-total">${money(event.ticketPrice)}</strong></p>
      <button id="book-event" class="button" ${event.availableTickets < 1 ? "disabled" : ""}>${event.availableTickets < 1 ? "Sold out" : "Book tickets"}</button>
      <p class="demo-note">Book now to reserve your tickets. No payment is required.</p>
      <div id="booking-message" class="form-message" role="status"></div></aside></div>`;
  const quantityInput = document.getElementById("ticket-quantity");
  quantityInput.addEventListener("input", () => {
    const quantity = Number(quantityInput.value);
    document.getElementById("ticket-total").textContent = Number.isInteger(quantity) && quantity > 0
      ? money(Number(event.ticketPrice) * quantity)
      : "Enter a valid quantity";
  });
  document.getElementById("book-event").addEventListener("click", async buttonEvent => {
    const button = buttonEvent.currentTarget;
    const user = await requireAuth();
    if (!user) return;
    const quantity = Number(document.getElementById("ticket-quantity").value);
    if (!Number.isInteger(quantity) || quantity < 1 || quantity > Math.min(20, event.availableTickets)) {
      document.getElementById("booking-message").textContent = "Choose a valid number of available tickets.";
      document.getElementById("booking-message").classList.add("error");
      return;
    }
    button.disabled = true;
    try {
      const booking = await api("/bookings", send("POST", { eventId: event.id, ticketQuantity: quantity }));
      location.href = bookingsUrl(booking.bookingReference);
    } catch (error) {
      button.disabled = false;
      document.getElementById("booking-message").textContent = error.message;
      document.getElementById("booking-message").classList.add("error");
    }
  });
}
function saveSession(result) {
  localStorage.setItem("eventhub_token", result.token);
  localStorage.setItem("eventhub_user", JSON.stringify(result.user));
}
function redirectForRole(user) {
  const destination = new URLSearchParams(location.search).get("return");
  if (destination && new URL(destination, location.href).origin === location.origin) location.href = destination;
  else if (user.role === "ADMIN") location.href = "admin.html";
  else if (user.role === "ORGANIZER") location.href = "dashboard.html";
  else location.href = "events.html";
}
function bindAuthForms() {
  const login = document.getElementById("login-form");
  login?.addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    try {
      const result = await api("/auth/login", send("POST", { email: data.get("email"), password: data.get("password") }));
      saveSession(result);
      redirectForRole(result.user);
    } catch (error) { setMessage(form, error.message, true); }
  });
  const register = document.getElementById("register-form");
  register?.addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    if (data.get("password") !== data.get("confirmPassword")) { setMessage(form, "Passwords do not match.", true); return; }
    try {
      const result = await api("/auth/register", send("POST", {
        fullName: data.get("fullName"), email: data.get("email"),
        phoneNumber: data.get("phoneNumber"), password: data.get("password")
      }));
      saveSession(result);
      location.href = "events.html";
    } catch (error) { setMessage(form, error.message, true); }
  });
}
async function loadBookings() {
  const host = document.getElementById("my-bookings");
  host.innerHTML = `<div class="loading">Loading bookings…</div>`;
  if (!await requireAuth()) return;
  const bookings = await api("/bookings/my");
  const bookingReference = new URLSearchParams(location.search).get("booking");
  const recentBooking = bookings.find(booking => booking.bookingReference === bookingReference);
  host.innerHTML = bookings.length ? bookings.map(booking => {
    const statusLabel = booking.bookingStatus === "CONFIRMED" ? "BOOKED" : booking.bookingStatus;
    const image = booking.imageUrl
      ? `<img src="${escapeHtml(booking.imageUrl)}" alt="${escapeHtml(booking.eventName)}" loading="lazy" onerror="this.remove()">`
      : `<span class="event-glyph" aria-hidden="true">${GLYPHS[booking.category] || "✦"}</span>`;
    return `<article class="booking-row"><div class="booking-event-image event-image" data-category="${escapeHtml(booking.category)}">${image}</div><div class="booking-details"><h3>${escapeHtml(booking.eventName)}</h3>
      <p>${dateLabel(booking.eventDate, booking.startTime)} · ${escapeHtml(booking.venue)}</p>
      <p>${booking.ticketQuantity} ticket${booking.ticketQuantity === 1 ? "" : "s"} · ${money(booking.ticketPrice)} per ticket · Total ${money(booking.totalPrice)}</p>
      <p>Booking ID: <strong>${escapeHtml(booking.id)}</strong> · Reference: <strong>${escapeHtml(booking.bookingReference)}</strong></p>
      <p>Booked ${new Date(booking.bookedAt).toLocaleDateString()}</p>
      <button class="small-button" data-print-ticket>Print ticket</button></div>
      <div class="row-actions"><span class="status ${booking.bookingStatus === "CANCELLED" ? "cancelled" : ""}">${escapeHtml(statusLabel)}</span>
      ${booking.bookingStatus !== "CANCELLED" && booking.eventDate >= new Date().toISOString().slice(0, 10) ? `<button class="small-button danger" data-cancel-booking="${booking.id}">Cancel booking</button>` : ""}</div></article>`;
  }).join("") : `<div class="empty-state">No bookings found. <a href="events.html">Find an event you love</a>.</div>`;
  if (recentBooking) {
    showToast("Ticket booked successfully!");
    history.replaceState(null, "", location.pathname);
  }
  if (host.dataset.actionsBound) return;
  host.dataset.actionsBound = "true";
  host.addEventListener("click", async event => {
    const button = event.target.closest("[data-cancel-booking]");
    if (event.target.closest("[data-print-ticket]")) { window.print(); return; }
    if (!button || !confirm("Cancel this booking? The tickets will return to the event inventory.")) return;
    button.disabled = true;
    try { await api(`/bookings/${button.dataset.cancelBooking}/cancel`, { method: "PATCH" }); showToast("Booking cancelled"); await loadBookings(); }
    catch (error) { button.disabled = false; showToast(error.message, true); }
  });
}
async function loadOrganizer() {
  const user = await requireAuth();
  if (!user) return;
  if (!["ORGANIZER", "ADMIN"].includes(user.role)) { showToast("An organizer account is required.", true); location.href = "events.html"; return; }
  const [stats, events] = await Promise.all([api("/organizer/dashboard"), api("/organizer/events")]);
  document.getElementById("organizer-stats").innerHTML = `<div class="stat-card"><span>Your events</span><strong>${stats.events}</strong></div><div class="stat-card"><span>Total bookings</span><strong>${stats.bookings}</strong></div>`;
  const host = document.getElementById("organizer-events");
  const render = list => host.innerHTML = list.length ? list.map(item => `<article class="organizer-event"><div><h3>${escapeHtml(item.name)} ${item.cancelled ? '<span class="status cancelled">CANCELLED</span>' : ""}</h3><p>${dateLabel(item.eventDate, item.startTime)} · ${escapeHtml(item.venue)}</p><p>${item.availableTickets} / ${item.totalTickets} tickets remaining · ${money(item.ticketPrice)}</p><div class="row-actions"><button class="small-button" data-attendees="${item.id}">Attendees</button>${item.cancelled ? "" : `<button class="small-button" data-edit-event="${item.id}">Edit</button><button class="small-button danger" data-delete-event="${item.id}">Delete</button>`}</div><div id="attendees-${item.id}"></div></div></article>`).join("") : `<div class="empty-state">You haven't created any events yet.</div>`;
  render(events);
  document.getElementById("event-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const payload = Object.fromEntries(data.entries());
    payload.totalTickets = Number(payload.totalTickets);
    try {
      await api("/events", send("POST", payload));
      form.reset(); setMessage(form, "Your event is live."); showToast("Event published");
      const [newStats, updated] = await Promise.all([api("/organizer/dashboard"), api("/organizer/events")]);
      document.getElementById("organizer-stats").innerHTML = `<div class="stat-card"><span>Your events</span><strong>${newStats.events}</strong></div><div class="stat-card"><span>Total bookings</span><strong>${newStats.bookings}</strong></div>`;
      render(updated);
    } catch (error) { setMessage(form, error.message, true); }
  });
  host.addEventListener("click", async event => {
    const attendees = event.target.closest("[data-attendees]");
    const edit = event.target.closest("[data-edit-event]");
    const remove = event.target.closest("[data-delete-event]");
    try {
      if (attendees) {
        const list = await api(`/organizer/events/${attendees.dataset.attendees}/attendees`);
        const box = document.getElementById(`attendees-${attendees.dataset.attendees}`);
        box.innerHTML = `<div class="attendee-panel">${list.length ? list.map(item => `<p>${escapeHtml(item.userName)} · ${escapeHtml(item.userEmail)} · ${item.ticketQuantity} tickets · ${escapeHtml(item.bookingStatus)}</p>`).join("") : "No attendees yet."}</div>`;
      } else if (edit) {
        const item = await api(`/events/${edit.dataset.editEvent}`);
        const name = prompt("Event name", item.name);
        if (name === null) return;
        const price = prompt("Ticket price in ₹", item.ticketPrice);
        if (price === null || !Number.isFinite(Number(price)) || Number(price) < 0) { showToast("Enter a valid ticket price.", true); return; }
        await api(`/events/${item.id}`, send("PUT", {
          name, description: item.description, category: item.category, eventDate: item.eventDate,
          startTime: item.startTime, venue: item.venue, imageUrl: item.imageUrl,
          ticketPrice: price, totalTickets: item.totalTickets
        }));
        showToast("Event updated");
        render(await api("/organizer/events"));
      } else if (remove) {
        if (!confirm("Delete this event? This is permanent if it has no booking history.")) return;
        await api(`/events/${remove.dataset.deleteEvent}`, { method: "DELETE" });
        showToast("Event deleted");
        render(await api("/organizer/events"));
      }
    } catch (error) { showToast(error.message, true); }
  });
}
function table(headers, rows) {
  if (!rows.length) return `<div class="empty-state">Nothing to show yet.</div>`;
  return `<table class="data-table"><thead><tr>${headers.map(header => `<th>${header}</th>`).join("")}</tr></thead><tbody>${rows.join("")}</tbody></table>`;
}
async function loadAdmin() {
  const user = await requireAuth();
  if (!user) return;
  if (user.role !== "ADMIN") { showToast("Administrator access is required.", true); location.href = "events.html"; return; }
  const [stats, users, bookings, events] = await Promise.all([api("/admin/dashboard"), api("/admin/users"), api("/admin/bookings"), api("/admin/events")]);
  document.getElementById("admin-stats").innerHTML = Object.entries({ "Total users": stats.users, "Total events": stats.events, "Total bookings": stats.bookings, "Confirmed bookings": stats.confirmedBookings }).map(([label, value]) => `<div class="stat-card"><span>${label}</span><strong>${value}</strong></div>`).join("");
  document.getElementById("admin-users").innerHTML = table(["User", "Email", "Role", "Joined"], users.map(person => `<tr><td>${escapeHtml(person.fullName)}</td><td>${escapeHtml(person.email)}</td><td><select data-user-role="${person.id}" aria-label="Role for ${escapeHtml(person.fullName)}">${["USER", "ORGANIZER", "ADMIN"].map(role => `<option ${person.role === role ? "selected" : ""}>${role}</option>`).join("")}</select></td><td>${new Date(person.createdAt).toLocaleDateString()}</td></tr>`));
  document.getElementById("admin-bookings").innerHTML = table(["Reference", "Attendee", "Event", "Tickets", "Total", "Status"], bookings.map(item => `<tr><td>${escapeHtml(item.bookingReference)}</td><td>${escapeHtml(item.userName)}</td><td>${escapeHtml(item.eventName)}</td><td>${item.ticketQuantity}</td><td>${money(item.totalPrice)}</td><td>${escapeHtml(item.bookingStatus)}</td></tr>`));
  document.getElementById("admin-events").innerHTML = table(["Event", "Organizer", "Date", "Tickets left", "Price", "Status / action"], events.map(item => `<tr><td>${escapeHtml(item.name)}</td><td>${escapeHtml(item.organizerName)}</td><td>${dateLabel(item.eventDate)}</td><td>${item.availableTickets} / ${item.totalTickets}</td><td>${money(item.ticketPrice)}</td><td>${item.cancelled ? '<span class="status cancelled">CANCELLED</span>' : `<button class="small-button danger" data-admin-cancel="${item.id}">Cancel event</button>`}</td></tr>`));
  document.getElementById("admin-users").addEventListener("change", async event => {
    const select = event.target.closest("[data-user-role]");
    if (!select) return;
    try { await api(`/admin/users/${select.dataset.userRole}/role`, send("PATCH", { role: select.value })); showToast("User role updated"); }
    catch (error) { showToast(error.message, true); }
  });
  document.getElementById("admin-events").addEventListener("click", async event => {
    const button = event.target.closest("[data-admin-cancel]");
    if (!button || !confirm("Cancel this event? It will no longer accept bookings.")) return;
    try { await api(`/admin/events/${button.dataset.adminCancel}/cancel`, { method: "PATCH" }); showToast("Event cancelled"); location.reload(); }
    catch (error) { showToast(error.message, true); }
  });
}
async function loadProfile() {
  const user = await requireAuth();
  if (!user) return;
  const profile = document.getElementById("profile-form");
  profile.elements.fullName.value = user.fullName;
  profile.elements.email.value = user.email;
  profile.elements.phoneNumber.value = user.phoneNumber || "";
  document.getElementById("profile-role").textContent = `Account role: ${user.role}`;
  profile.addEventListener("submit", async event => {
    event.preventDefault();
    try {
      const updated = await api("/auth/me", send("PUT", {
        fullName: profile.elements.fullName.value,
        phoneNumber: profile.elements.phoneNumber.value
      }));
      localStorage.setItem("eventhub_user", JSON.stringify(updated));
      setMessage(profile, "Profile updated.");
      renderHeader();
    } catch (error) { setMessage(profile, error.message, true); }
  });
  document.getElementById("password-form").addEventListener("submit", async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    try {
      await api("/auth/me/password", send("PUT", { currentPassword: data.get("currentPassword"), newPassword: data.get("newPassword") }));
      form.reset(); setMessage(form, "Password changed successfully.");
    } catch (error) { setMessage(form, error.message, true); }
  });
}
async function initialize() {
  renderHeader();
  renderFooter();
  bindAuthForms();
  const page = document.body.dataset.page;
  try {
    if (page === "home") await loadHome();
    else if (page === "events") await loadEvents();
    else if (page === "event") await loadEvent();
    else if (page === "bookings") await loadBookings();
    else if (page === "dashboard") await loadOrganizer();
    else if (page === "admin") await loadAdmin();
    else if (page === "profile") await loadProfile();
  } catch (error) {
    showToast(error.message, true);
    const host = document.querySelector("main");
    if (host && page === "event") host.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}. <a href="events.html">Back to events</a></div>`;
    if (page === "home") document.getElementById("featured-events").innerHTML = `<div class="empty-state">Events are temporarily unavailable. Please try again soon.</div>`;
    if (page === "bookings") document.getElementById("my-bookings").innerHTML = `<div class="empty-state error">${escapeHtml(error.message)}. Please try again.</div>`;
  }
}
document.addEventListener("DOMContentLoaded", initialize);
