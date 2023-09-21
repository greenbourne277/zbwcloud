import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";

Vue.use(VueRouter);

const routes: Array<RouteConfig> = [
  {
    path: "/ui",
    name: "ui",
    component: () => import("../components/ItemList.vue"),
  },
  {
    path: "/api/v1",
    name: "api",
    component: () => import("../components/API.vue"),
  },
];

const router = new VueRouter({
  mode: "history",
  base: process.env.BASE_URL,
  routes,
});

export default router;
