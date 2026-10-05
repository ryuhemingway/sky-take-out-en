<script setup>
import { ref, onMounted } from "vue";
import { api } from "../api";
import { RefreshCw, Eye } from "lucide-vue-next";
import Modal from "../components/Modal.vue";
const rows = ref([]),
  status = ref(""),
  detail = ref(null),
  loading = ref(false),
  error = ref(""),
  labels = {
    1: "Pending Payment",
    2: "Awaiting Acceptance",
    3: "Accepted",
    4: "On the Way",
    5: "Completed",
    6: "Cancelled",
  };
async function load() {
  if (loading.value) return;
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api(
      `/api/admin/order/page${status.value ? "?status=" + status.value : ""}`,
    );
  } catch (e) {
    error.value = e.message || "Failed to load orders";
  } finally {
    loading.value = false;
  }
}
async function view(id) {
  detail.value = await api(`/api/admin/order/${id}`);
}
async function action(id, name) {
  await api(`/api/admin/order/${id}/${name}`, { method: "PUT" });
  await load();
}
onMounted(load);
</script>
<template>
  <div class="page-head">
    <div>
      <h1>Orders</h1>
      <p>Handle new orders and track fulfillment</p>
    </div>
    <button class="secondary" :disabled="loading" @click="load">
      <RefreshCw :size="17" />{{ loading ? "Loading" : "Refresh" }}
    </button>
  </div>
  <p v-if="error" class="error">{{ error }}</p>
  <div class="filters">
    <select v-model="status" @change="load">
      <option value="">All Statuses</option>
      <option v-for="(v, k) in labels" :value="k">{{ v }}</option>
    </select>
  </div>
  <div class="table-wrap">
    <table>
      <thead>
        <tr>
          <th>Order No.</th>
          <th>Delivery Info</th>
          <th>Notes</th>
          <th>Amount</th>
          <th>Order Time</th>
          <th>Status</th>
          <th class="actions">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in rows">
          <td>
            <strong>{{ r.number }}</strong>
          </td>
          <td>
            {{ r.consignee }}<small>{{ r.phone }}</small>
          </td>
          <td class="remark-cell">{{ r.remark || "None" }}</td>
          <td class="price">¥ {{ r.amount }}<small v-if="r.discountAmount">Discount ¥{{ r.discountAmount }}</small></td>
          <td>{{ r.orderTime?.replace("T", " ") }}</td>
          <td>
            <span class="status" :class="'s' + r.status">{{
              labels[r.status]
            }}</span>
            <span v-if="r.reminder === 1" class="status s6">Reminder</span>
          </td>
          <td class="actions order-actions">
            <button title="Details" @click="view(r.id)"><Eye /></button
            ><button
              v-if="r.status === 2"
              class="text-action"
              @click="action(r.id, 'confirm')"
            >
              Accept</button
            ><button
              v-if="r.status === 3"
              class="text-action"
              @click="action(r.id, 'delivery')"
            >
              Deliver</button
            ><button
              v-if="r.status === 4"
              class="text-action"
              @click="action(r.id, 'complete')"
            >
              Complete</button
            ><button
              v-if="![5, 6].includes(r.status)"
              class="text-action danger"
              @click="action(r.id, 'cancel')"
            >
              Cancel
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
  <Modal v-if="detail" title="Order Details" @close="detail = null"
    ><div class="order-detail">
      <dl>
        <dt>Order No.</dt>
        <dd>{{ detail.order.number }}</dd>
        <dt>Recipient</dt>
        <dd>{{ detail.order.consignee }} {{ detail.order.phone }}</dd>
        <dt>Address</dt>
        <dd>{{ detail.order.address }}</dd>
        <dt>Notes</dt>
        <dd>{{ detail.order.remark || "None" }}</dd>
        <dt>Coupon</dt>
        <dd>{{ detail.order.couponCode ? `${detail.order.couponCode}, discount ¥${detail.order.discountAmount}` : "None" }}</dd>
      </dl>
      <h4>Items</h4>
      <div class="detail-line" v-for="d in detail.details">
        <span>{{ d.name }} × {{ d.number }}</span
        ><strong>¥ {{ (d.amount * d.number).toFixed(2) }}</strong>
      </div>
      <div class="detail-total">
        <span>Total</span><strong>¥ {{ detail.order.amount }}</strong>
      </div>
    </div></Modal
  >
</template>
