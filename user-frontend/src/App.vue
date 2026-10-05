<script setup>
import { ref, onMounted, onUnmounted, computed } from "vue";
import { api, body } from "./api";
import { connectUser } from "./realtime";
import {
  ShoppingBag,
  Plus,
  Minus,
  MapPin,
  ClipboardList,
  UserRound,
  LogIn,
  ChevronRight,
  Store,
  Clock3,
  X,
  UtensilsCrossed,
  Search,
  Heart,
  Tag,
  House,
  Bell,
  Trash2,
  Wifi,
} from "lucide-vue-next";
const logged = ref(!!localStorage.getItem("user_token")),
  username = ref("dev-user-001"),
  name = ref("Test User"),
  loginError = ref(""),
  categories = ref([]),
  dishes = ref([]),
  setmeals = ref([]),
  cart = ref([]),
  addresses = ref([]),
  orders = ref([]),
  categoryId = ref(null),
  tab = ref("menu"),
  showLogin = ref(!logged.value),
  showAddress = ref(false),
  shopStatus = ref(1),
  toast = ref(""),
  settlementError = ref(""),
  orderRemark = ref(""),
  search = ref(""),
  selectedDish = ref(null),
  coupons = ref([]),
  selectedCoupon = ref(null),
  orderQuery = ref(""),
  orderStatus = ref(""),
  connected = ref(false),
  showNotifications = ref(false),
  notifications = ref([]),
  form = ref({ consignee: "", phone: "", detail: "", sex: "M", label: "Home" }),
  selectedAddress = ref(null);
let socket;
const cartCount = computed(() => cart.value.reduce((n, x) => n + x.number, 0)),
  cartTotal = computed(() =>
    cart.value.reduce((n, x) => n + Number(x.amount) * x.number, 0),
  ),
  activeCategory = computed(
    () =>
      categories.value.find((x) => x.id === categoryId.value)?.name ||
      "All Dishes",
  );
const filteredDishes = computed(() => {
  const keyword = search.value.trim().toLowerCase();
  return keyword
    ? dishes.value.filter((d) => `${d.name}${d.description || ""}`.toLowerCase().includes(keyword))
    : dishes.value;
});
const discount = computed(() => {
  if (!selectedCoupon.value) return 0;
  return cartTotal.value >= Number(selectedCoupon.value.threshold)
    ? Number(selectedCoupon.value.discount)
    : 0;
});
const payableTotal = computed(() => Math.max(0, cartTotal.value - discount.value));
const unreadNotifications = computed(() => notifications.value.filter(item => !item.read).length);
function toggleNotifications() {
  showNotifications.value = !showNotifications.value;
  if (showNotifications.value) notifications.value.forEach(item => item.read = true);
}
function dishImage(d) {
  if (d.image) return d.image;
  const images = {
    35: "/images/dishes/35-beef.jpg",
    34: "/images/dishes/34-egg-soup.jpg",
    27: "/images/dishes/27-rice.jpg",
    33: "/images/dishes/33-peanuts.jpg",
    32: "/images/dishes/32-guobaorou.jpg",
    31: "/images/dishes/31-sweet-sour-pork.jpg",
    29: "/images/dishes/29-salad.jpg",
    3: "/images/dishes/3-kungpao.jpg",
    26: "/images/dishes/26-boiled-pork.jpg",
    24: "/images/dishes/24-noodles.jpg",
    28: "/images/dishes/28-cola.jpg",
  };
  return images[d.id] || "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=640&q=82";
}
function promoClick() {
  if (!logged.value) { showLogin.value = true; return; }
  if (!cart.value.length) { msg("Add dishes first. Coupons apply to orders of ¥30 or more"); return; }
  tab.value = "checkout";
}
async function login() {
  try {
    const d = await api("/api/user/login", {
      method: "POST",
      body: body({ username: username.value, name: name.value }),
    });
    localStorage.setItem("user_token", d.token);
    logged.value = true;
    showLogin.value = false;
    startRealtime();
    loadAll();
  } catch (e) {
    loginError.value = e.message;
  }
}
async function loadAll() {
  categories.value = await api("/api/user/menu/categories?type=1");
  categoryId.value = null;
  await loadMenu();
  await loadCart();
  await loadAddresses();
  await loadOrders();
  try { coupons.value = await api("/api/user/coupon/list"); } catch {}
  try {
    shopStatus.value = (await api("/api/shop/status")).status;
  } catch {}
}
async function loadMenu() {
  const id = categoryId.value;
  dishes.value = await api(
    `/api/user/menu/dishes${id ? "?categoryId=" + id : ""}`,
  );
  setmeals.value = await api("/api/user/menu/setmeals");
}
async function loadCart() {
  if (logged.value) cart.value = await api("/api/user/shoppingCart/list");
}
async function loadAddresses() {
  if (logged.value) addresses.value = await api("/api/user/addressBook/list");
}
async function loadOrders() {
  if (logged.value) {
    const params = new URLSearchParams();
    if (orderStatus.value) params.set("status", orderStatus.value);
    if (orderQuery.value.trim()) params.set("number", orderQuery.value.trim());
    const list = await api(`/api/user/order/list${params.size ? "?" + params : ""}`);
    orders.value = await Promise.all(list.map(async order => {
      try { return { ...order, delivery: await api(`/api/user/delivery/order/${order.id}`) }; }
      catch { return order; }
    }));
  }
}
async function addDish(d) {
  if (!logged.value) {
    showLogin.value = true;
    return;
  }
  try {
    await api("/api/user/shoppingCart/add", {
      method: "POST",
      body: body({ dishId: d.id, number: 1 }),
    });
    await loadCart();
    msg("Added to cart");
  } catch (e) {
    msg(e.message || "Failed to add to cart");
  }
}
async function updateCart(item, n) {
  try {
    if (n < 1) {
      await api(`/api/user/shoppingCart/sub?dishId=${item.dishId}`, {
        method: "DELETE",
      });
    } else
      await api("/api/user/shoppingCart/number", {
        method: "PUT",
        body: body({ dishId: item.dishId, number: n }),
      });
    await loadCart();
  } catch (e) {
    msg(e.message || "Failed to update cart");
  }
}
function msg(x) {
  toast.value = x;
  setTimeout(() => (toast.value = ""), 1800);
}
async function saveAddress() {
  await api("/api/user/addressBook", {
    method: "POST",
    body: body(form.value),
  });
  showAddress.value = false;
  form.value = { consignee: "", phone: "", detail: "", sex: "M", label: "Home" };
  await loadAddresses();
  msg("Address saved");
}
async function submitOrder() {
  if (!shopStatus.value) {
    settlementError.value = "Shop is closed, checkout failed";
    return;
  }
  if (!selectedAddress.value) {
    msg("Please select a delivery address");
    return;
  }
  if (!cart.value.length) {
    msg("Cart is empty");
    return;
  }
  try {
    const d = await api("/api/user/order/submit", {
      method: "POST",
      body: body({
        addressBookId: selectedAddress.value.id,
        remark: orderRemark.value.trim(),
        couponCode: discount.value > 0 ? selectedCoupon.value.code : null,
      }),
    });
    await loadCart();
    await loadOrders();
    orderRemark.value = "";
    tab.value = "orders";
    msg(`Order placed, discount ¥${d.discount || 0}; total paid ¥${d.amount}`);
  } catch (error) {
    if ((error.message || "").includes("Shop is closed")) {
      shopStatus.value = 0;
      settlementError.value = "Shop is closed, checkout failed";
    } else {
      settlementError.value = `Checkout failed: ${error.message || "Please try again later"}`;
    }
  }
}
async function pay(o) {
  await api(`/api/user/order/${o.id}/pay`, { method: "PUT" });
  await loadOrders();
  msg("Payment successful");
}
async function remind(o) {
  try {
    await api(`/api/user/order/${o.id}/reminder`, { method: "PUT" });
    await loadOrders();
    msg("Shop notified of your reminder");
  } catch (e) {
    msg(e.message || "Failed to send reminder");
  }
}
async function cancel(o) {
  await api(`/api/user/order/${o.id}/cancel`, { method: "PUT" });
  await loadOrders();
  msg("Order cancelled");
}
async function deleteOrder(o) {
  if (!confirm(`Delete past order #${o.number}?`)) return;
  try { await api(`/api/user/order/${o.id}`, { method: "DELETE" }); await loadOrders(); msg("Past order deleted"); }
  catch (e) { msg(e.message || "Delete failed"); }
}
function startRealtime() {
  socket?.deactivate();
  socket = connectUser(async (event) => {
    await loadOrders();
    let text = "Order status updated";
    if (event.type === "RIDER_ASSIGNED") text = "Rider assigned, heading to the shop";
    else if (event.type === "DELIVERY_PICKED") text = "Rider picked up your order, out for delivery";
    else if (event.type === "DELIVERY_COMPLETED") text = "Order delivered";
    notifications.value.unshift({ text, time: new Date().toLocaleTimeString(), read: false });
    notifications.value = notifications.value.slice(0, 20); msg(text);
  }, value => connected.value = value);
}
onMounted(() => { if (logged.value) { loadAll(); startRealtime(); } });
onUnmounted(() => socket?.deactivate());
</script>
<template>
  <div class="app">
    <header class="top">
      <div class="top-inner">
        <div class="brand">
          <span><Store :size="21" /></span><strong>Sky Take-Out</strong>
        </div>
        <div class="shop-state">
          <i :class="{ closed: !shopStatus }"></i
          >{{ shopStatus ? "Open" : "Closed" }}
        </div>
        <div v-if="logged" class="user-notice-wrap">
          <button class="notice-button" :title="connected ? 'Live notifications connected' : 'Connecting to live notifications'" @click="toggleNotifications">
            <Bell :size="19"/><i class="connection-dot" :class="{online:connected}"></i><b v-if="unreadNotifications">{{ unreadNotifications }}</b>
          </button>
          <section v-if="showNotifications" class="user-notice-panel">
            <header><strong>Delivery Updates</strong><small><Wifi :size="13"/>{{connected?'Live connection OK':'Reconnecting'}}</small></header>
            <p v-if="!notifications.length">Rider assignment, pickup and delivery updates appear here in real time.</p>
            <button v-for="item in notifications" :key="item.time+item.text"><span>{{item.text}}</span><small>{{item.time}}</small></button>
          </section>
        </div>
        <button class="user-btn" @click="showLogin = true">
          <UserRound :size="18" />{{ logged ? "My Account" : "Log In" }}
        </button>
      </div>
    </header>
    <main>
      <section v-if="!logged" class="welcome">
        <div>
          <span class="kicker">TODAY'S MENU</span>
          <h1>What are you craving?</h1>
          <p>Freshly made and delivered hot to your door.</p>
          <button class="primary" @click="showLogin = true">
            <LogIn :size="18" />Start Ordering
          </button>
        </div>
        <div class="welcome-art">
          <ShoppingBag :size="100" stroke-width="1.2" />
        </div>
      </section>
      <template v-else
        ><div class="layout">
          <aside class="categories">
            <h3>Menu</h3>
            <button
              :class="{ active: !categoryId }"
              @click="
                categoryId = null;
                loadMenu();
              "
            >
              All Dishes</button
            ><button
              v-for="c in categories"
              :class="{ active: categoryId === c.id }"
              @click="
                categoryId = c.id;
                loadMenu();
              "
            >
              {{ c.name }}<ChevronRight :size="15" />
            </button>
            <div class="aside-note">
              <Clock3 :size="17" /><span
                >Est. delivery<br /><strong>30 - 45 min</strong></span
              >
            </div>
          </aside>
          <section class="menu">
            <div class="mobile-search"><Search :size="18" /><input v-model="search" placeholder="Search for a dish" /></div>
            <button class="promo-strip" @click="promoClick"><div><Tag :size="22" /><span><b>Deals of the Day</b><small>¥5 off ¥30, new customer first-order discount</small></span></div><strong>View ›</strong></button>
            <div class="activity-row"><button class="activity-card orange" @click="promoClick"><span>New Users</span><b>¥5 off first order</b><small>Claim now ›</small></button><button class="activity-card yellow" @click="promoClick"><span>Limited Time</span><b>¥10 off ¥60</b><small>Use now ›</small></button><button class="activity-card green" @click="tab = 'orders'; loadOrders()"><span>Worry-Free Delivery</span><b>Remind if late</b><small>View orders ›</small></button></div>
            <div class="section-heading">
              <div>
                <span class="kicker">FRESHLY MADE</span>
                <h1>{{ activeCategory }}</h1>
              </div>
              <button class="cart-trigger" @click="tab = 'cart'">
                <ShoppingBag :size="20" /><b v-if="cartCount">{{ cartCount }}</b
                >Cart
              </button>
            </div>
            <div class="food-grid">
              <article v-for="d in filteredDishes" class="food-card" @click="selectedDish = d">
                <div class="food-image">
                  <img :src="dishImage(d)" :alt="d.name" />
                </div>
                <div class="food-info">
                  <h3>{{ d.name }}</h3>
                  <p>{{ d.description || "Selected ingredients, made to order" }}</p>
                  <div>
                    <strong>¥ {{ d.price }}</strong
                    ><button class="add-btn" @click.stop="addDish(d)">
                      <Plus :size="19" />
                    </button>
                  </div>
                </div>
              </article>
              <article v-for="s in setmeals" class="food-card">
                <div class="food-image setmeal"><span>Set Meal</span></div>
                <div class="food-info">
                  <h3>{{ s.name }}</h3>
                  <p>{{ s.description || "A hassle-free combo" }}</p>
                  <div>
                    <strong>¥ {{ s.price }}</strong>
                  </div>
                </div>
              </article>
            </div>
          </section>
        </div>
        <nav class="bottom-nav">
          <button :class="{ active: tab === 'menu' }" @click="tab = 'menu'">
            <UtensilsCrossed /><span>Menu</span></button
          ><button :class="{ active: tab === 'cart' }" @click="tab = 'cart'">
            <ShoppingBag /><span
              >Cart<em v-if="cartCount">{{ cartCount }}</em></span
            ></button
          ><button
            :class="{ active: tab === 'orders' }"
            @click="
              tab = 'orders';
              loadOrders();
            "
          >
            <ClipboardList /><span>Orders</span>
          </button>
        </nav>
        <div v-if="tab === 'cart'" class="sheet">
          <div class="sheet-head">
            <div>
              <span class="kicker">YOUR ORDER</span>
              <h2>Cart</h2>
            </div>
            <button class="icon-btn" @click="tab = 'menu'"><X /></button>
          </div>
          <div v-if="!cart.length" class="empty">
            Your cart is empty. Pick something from the menu.
          </div>
          <div v-for="item in cart" class="cart-row">
            <div class="mini-image"><UtensilsCrossed :size="17" /></div>
            <div class="cart-name">
              <strong>{{ item.name }}</strong
              ><small>¥ {{ item.amount }}</small>
            </div>
            <div class="stepper">
              <button @click="updateCart(item, item.number - 1)">
                <Minus /></button
              ><b>{{ item.number }}</b
              ><button @click="updateCart(item, item.number + 1)">
                <Plus />
              </button>
            </div>
          </div>
          <div v-if="cart.length" class="checkout">
            <div>
              <small>Total</small><strong>¥ {{ cartTotal.toFixed(2) }}</strong>
            </div>
            <button class="primary" @click="tab = 'checkout'">Checkout</button>
          </div>
        </div>
        <button v-if="cartCount && tab === 'menu'" class="floating-cart" @click="tab = 'cart'"><ShoppingBag :size="21" /><span>Cart</span><b>{{ cartCount }} items</b><strong>¥ {{ cartTotal.toFixed(2) }}</strong></button>
        <div v-if="tab === 'checkout'" class="sheet">
          <div class="sheet-head">
            <div>
              <span class="kicker">CHECKOUT</span>
              <h2>Confirm Order</h2>
            </div>
            <button class="icon-btn" @click="tab = 'cart'"><X /></button>
          </div>
          <div class="address-list">
            <button
              v-for="a in addresses"
              :class="{ selected: selectedAddress?.id === a.id }"
              @click="selectedAddress = a"
            >
              <MapPin :size="18" /><span
                ><strong>{{ a.consignee }} {{ a.phone }}</strong
                ><small>{{ a.detail }}</small></span
              ><i></i></button
            ><button class="add-address" @click="showAddress = true">
              <Plus :size="18" />Add Address
            </button>
          </div>
          <div v-if="coupons.length" class="coupon-box">
            <div class="coupon-title"><span>Coupon</span><small>Save ¥{{ discount.toFixed(2) }}</small></div>
            <button v-for="c in coupons" class="coupon-option" :class="{selected:selectedCoupon?.code===c.code}" @click="selectedCoupon = selectedCoupon?.code===c.code ? null : c"><span><b>¥{{ c.discount }}</b><small>{{ c.name }}</small></span><i>{{ cartTotal >= Number(c.threshold) ? 'Available' : `Spend ${c.threshold}+` }}</i></button>
          </div>
          <label class="order-remark">
            <span>Order Notes</span>
            <textarea
              v-model="orderRemark"
              maxlength="255"
              placeholder="Taste, utensils or delivery requests (optional)"
            ></textarea>
            <small>{{ orderRemark.length }}/255</small>
          </label>
          <div class="checkout">
            <div>
              <small>Amount Due</small
              ><strong>¥ {{ payableTotal.toFixed(2) }}</strong>
            </div>
            <button
              class="primary"
              @click="submitOrder"
            >
              {{ shopStatus ? "Place Order" : "Shop Closed" }}
            </button>
          </div>
        </div>
        <div v-if="tab === 'orders'" class="sheet">
          <div class="sheet-head">
            <div>
              <span class="kicker">ORDER HISTORY</span>
              <h2>My Orders</h2>
            </div>
            <button class="icon-btn" @click="tab = 'menu'"><X /></button>
          </div>
          <div class="order-filters">
            <input v-model="orderQuery" placeholder="Search by order number" @keyup.enter="loadOrders" />
            <select v-model="orderStatus" @change="loadOrders"><option value="">All Statuses</option><option value="1">Pending Payment</option><option value="2">Awaiting Acceptance</option><option value="3">Accepted</option><option value="4">Out for Delivery</option><option value="5">Completed</option><option value="6">Cancelled</option></select>
            <button @click="loadOrders">Search</button>
          </div>
          <div v-if="!orders.length" class="empty">No orders yet.</div>
          <article v-for="o in orders" class="order-card">
            <div class="order-top">
              <strong>#{{ o.number }}</strong
              ><span>{{
                {
                  1: "Pending Payment",
                  2: "Awaiting Acceptance",
                  3: "Accepted",
                  4: "Out for Delivery",
                  5: "Completed",
                  6: "Cancelled",
                }[o.status]
              }}</span>
            </div>
            <p>{{ o.address }}</p>
            <p v-if="o.delivery" class="delivery-info">
              Delivery: {{ ({ 1: 'Rider Assigned', 2: 'On the Way', 3: 'Delivered' })[o.delivery.status] }}
              <span v-if="o.delivery.riderName"> · {{ o.delivery.riderName }} {{ o.delivery.riderPhone }}</span>
            </p>
            <div class="order-bottom">
              <strong>¥ {{ o.amount }}</strong
              ><button
                v-if="[2, 3, 4].includes(o.status) && o.reminder !== 1"
                class="text-btn"
                @click="remind(o)"
              >
                Remind</button
              ><button v-if="o.status === 1" class="text-btn" @click="pay(o)">
                Pay Now</button
              ><button
                v-if="o.status === 1"
                class="text-btn muted"
                @click="cancel(o)"
              >
                Cancel Order
              </button>
              <button v-if="[5,6].includes(o.status)" class="text-btn delete-order" @click="deleteOrder(o)"><Trash2 :size="14"/>Delete</button>
            </div>
          </article>
        </div></template
      >
    </main>
    <div v-if="showLogin" class="modal-backdrop">
      <section class="login-modal">
        <div class="login-icon"><UserRound /></div>
        <h2>Log in to start ordering</h2>
        <p>Use a dev account to enter the customer web UI</p>
        <label>Username<input v-model="username" /></label
        ><label>Name<input v-model="name" /></label>
        <p v-if="loginError" class="error">{{ loginError }}</p>
        <button class="primary wide" @click="login">Log in and start ordering</button>
      </section>
    </div>
    <div v-if="settlementError" class="modal-backdrop" @click.self="settlementError = ''">
      <section class="login-modal settlement-modal">
        <button class="icon-btn close" @click="settlementError = ''"><X /></button>
        <div class="settlement-icon"><Store :size="25" /></div>
        <h2>Checkout Failed</h2>
        <p>{{ settlementError }}</p>
        <button class="primary wide" @click="settlementError = ''; tab = 'menu'">Back to Menu</button>
      </section>
    </div>
    <div v-if="showAddress" class="modal-backdrop">
      <section class="login-modal address-modal">
        <button class="icon-btn close" @click="showAddress = false">
          <X />
        </button>
        <h2>Add Address</h2>
        <label>Recipient<input v-model="form.consignee" /></label
        ><label>Phone<input v-model="form.phone" /></label
        ><label>Address Details<input v-model="form.detail" /></label
        ><button class="primary wide" @click="saveAddress">Save Address</button>
      </section>
    </div>
    <div v-if="selectedDish" class="modal-backdrop" @click.self="selectedDish = null"><section class="dish-modal"><button class="icon-btn close" @click="selectedDish = null"><X /></button><img :src="dishImage(selectedDish)" :alt="selectedDish.name" /><div class="dish-modal-body"><span class="kicker">FRESHLY MADE</span><h2>{{ selectedDish.name }}</h2><p>{{ selectedDish.description || 'Selected ingredients, made to order' }}</p><div><strong>¥ {{ selectedDish.price }}</strong><button class="primary" @click="addDish(selectedDish);selectedDish=null"><Plus :size="18" />Add to Cart</button></div></div></section></div>
    <div v-if="toast" class="toast">{{ toast }}</div>
  </div>
</template>
