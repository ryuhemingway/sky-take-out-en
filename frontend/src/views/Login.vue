<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import { LogIn, Utensils } from "lucide-vue-next";
import { api, json } from "../api";
const router = useRouter(),
  username = ref("admin"),
  password = ref("123456"),
  loading = ref(false),
  error = ref("");
async function login() {
  loading.value = true;
  error.value = "";
  try {
    const data = await api("/api/admin/employee/login", {
      method: "POST",
      body: json({ username: username.value, password: password.value }),
    });
    localStorage.setItem("admin_token", data.token);
    localStorage.setItem("admin_name", data.name);
    router.push("/");
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
</script>
<template>
  <main class="login-page">
    <section class="login-brand">
      <div class="brand-mark"><Utensils :size="30" /></div>
      <h1>Sky Take-Out</h1>
      <p>Shop Operations Center</p>
    </section>
    <form class="login-panel" @submit.prevent="login">
      <div>
        <span class="eyebrow">ADMIN CONSOLE</span>
        <h2>Welcome back</h2>
        <p>Log in to manage dishes, set meals and orders</p>
      </div>
      <label>Account<input v-model="username" autocomplete="username" /></label
      ><label
        >Password<input
          v-model="password"
          type="password"
          autocomplete="current-password"
      /></label>
      <p v-if="error" class="error">{{ error }}</p>
      <button class="primary wide" :disabled="loading">
        <LogIn :size="18" />{{ loading ? "Logging in..." : "Log In" }}
      </button>
    </form>
  </main>
</template>
<style scoped>
.login-page { background: #fff5ef; }
.login-brand { min-height: 100vh; display: flex; flex-direction: column; justify-content: center; color: #fff; background: linear-gradient(90deg, rgba(116,42,26,.82), rgba(116,42,26,.18)), url('https://images.unsplash.com/photo-1526367790999-0150786686a2?auto=format&fit=crop&w=1400&q=85') center/cover; }
.brand-mark { background: #ed5a32; }
.login-panel { box-shadow: 0 18px 50px rgba(138,53,31,.12); }
.primary { background: #ed5a32; }
.primary:hover { background: #cf4625; }
@media (max-width: 800px) { .login-brand { min-height: auto; background-position: center 35%; } }
</style>
