import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import Login from './views/Login.vue'
import Admin from './views/Admin.vue'
import './style.css'
const router=createRouter({history:createWebHistory(),routes:[{path:'/login',component:Login},{path:'/',component:Admin}]})
router.beforeEach(to=>{if(to.path!='/login'&&!localStorage.getItem('admin_token'))return '/login'})
createApp(App).use(router).mount('#app')
