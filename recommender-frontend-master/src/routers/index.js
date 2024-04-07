import Vue from 'vue'
import Router from 'vue-router'
import BaseLayout from '../layouts/BaseLayout'
import Recommend from "../pages/Recommend";
import Rank from "../pages/Rank";
import Hot from "../pages/Hot";
import LFM from "../pages/LFM";
import History from "../pages/History"

Vue.use(Router);

export const constantRoutes = [
    {
        path: '/',
        component: BaseLayout,
        redirect: 'login',
        children: [
            {
                path: "/recommend",
                component: Recommend
            },
            {
                path:"/lfm",
                component:LFM,
            },
            {
                path: "/rank",
                component: Rank
            },
            {
                path: "/hot",
                component: Hot
            },
            {
                path:"/history",
                component:History
            },
            {
                path: '/movie/:mid',
                component: () => import("../pages/Movie")
            },
            {
                path: '/search',
                component: () => import('../pages/Search')
            }
        ]
    },
    {
        path: '/login',
        component: () => import('../pages/Login')
    },
    {
        path: '/register',
        component: () => import('../pages/Register')
    },
];

const router = new Router({
    mode: "history",
    routes: constantRoutes
});

export default router;
