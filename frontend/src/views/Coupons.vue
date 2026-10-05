<script setup>
import { ref, onMounted } from 'vue'
import { api, json } from '../api'
import { Plus, RefreshCw } from 'lucide-vue-next'

const rows = ref([])
const error = ref('')
const form = ref({ code: '', name: '', discountAmount: 5, thresholdAmount: 30, totalStock: 100,
  startTime: new Date().toISOString().slice(0, 16), endTime: new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 16) })

async function load() {
  try { rows.value = await api('/api/admin/coupon/page'); error.value = '' }
  catch (e) { error.value = e.message }
}
async function create() {
  try {
    await api('/api/admin/coupon', { method: 'POST', body: json(form.value) })
    form.value.code = ''; form.value.name = ''; await load()
  } catch (e) { error.value = e.message }
}
async function toggle(row) {
  await api(`/api/admin/coupon/${row.id}/status/${row.status ? 0 : 1}`, { method: 'PUT' }); await load()
}
onMounted(load)
</script>

<template>
  <div class="page-head"><div><h1>Coupons</h1><p>Create spend-and-save promotions and manage stock</p></div><button class="secondary" @click="load"><RefreshCw :size="17"/>Refresh</button></div>
  <p v-if="error" class="error">{{ error }}</p>
  <div class="filters coupon-form">
    <input v-model="form.code" placeholder="Code, e.g. SAVE20"><input v-model="form.name" placeholder="Promotion name">
    <input v-model.number="form.thresholdAmount" type="number" min="0" placeholder="Minimum spend">
    <input v-model.number="form.discountAmount" type="number" min="0.01" step="0.01" placeholder="Discount amount">
    <input v-model.number="form.totalStock" type="number" min="1" placeholder="Quantity">
    <input v-model="form.startTime" type="datetime-local"><input v-model="form.endTime" type="datetime-local">
    <button class="primary" @click="create"><Plus :size="16"/>Create Coupon</button>
  </div>
  <div class="table-wrap"><table><thead><tr><th>Code</th><th>Name</th><th>Rule</th><th>Stock</th><th>Validity</th><th>Status</th><th>Actions</th></tr></thead>
  <tbody><tr v-for="r in rows" :key="r.id"><td><strong>{{r.code}}</strong></td><td>{{r.name}}</td><td>Spend ¥{{r.thresholdAmount}} save ¥{{r.discountAmount}}</td><td>{{r.remainingStock}} / {{r.totalStock}}</td><td>{{r.startTime?.replace('T',' ')}}<small>to {{r.endTime?.replace('T',' ')}}</small></td><td><span class="status" :class="r.status?'s5':'s6'">{{r.status?'Enabled':'Disabled'}}</span></td><td><button class="text-action" @click="toggle(r)">{{r.status?'Disable':'Enable'}}</button></td></tr></tbody></table></div>
</template>
