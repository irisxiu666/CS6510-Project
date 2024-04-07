<template>
  <v-app id="inspire">
    <v-app-bar app clipped-left color="white" dark>
      <v-toolbar-title class="ml-0 pl-4" style="width: 300px">
        <span class="hidden-sm-and-down" style="color: black"
          >Movie Recommendation</span
        >
      </v-toolbar-title>
      <v-spacer></v-spacer>
    </v-app-bar>

    <v-main>
      <v-container class="fill-height" fluid>
        <v-row align="center" justify="center">
          <v-col cols="12" md="4" sm="8">
            <v-card class="elevation-12">
              <v-toolbar color="primary" dark flat>
                <v-toolbar-title>Register</v-toolbar-title>
                <v-spacer></v-spacer>
              </v-toolbar>
              <v-card-text>
                <v-form>
                  <v-text-field
                    label="Username"
                    name="login"
                    prepend-icon="mdi-account"
                    type="text"
                    v-model="username"
                  ></v-text-field>

                  <v-text-field
                    id="password"
                    label="Password"
                    name="password"
                    prepend-icon="mdi-lock"
                    type="password"
                    v-model="password"
                  ></v-text-field>
                </v-form>
                <v-card-actions>
                  <v-spacer></v-spacer>
                  <v-btn @click="onRegister" color="primary">Submit</v-btn>
                </v-card-actions>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </v-main>
  </v-app>
</template>

<script>
import { register } from '@/api/user';
// import md5 from 'js-md5';

export default {
  data: () => ({
    username: '',
    password: '',
  }),
  methods: {
    onRegister() {
      register({
        username: this.username,
        password: this.password,
      })
        .then((res) => {
          if (res.data.success) {
            this.$store.commit('showTips', { text: 'Register success' });
            this.$router.push('/login').catch(() => {});
          } else {
            this.$store.commit('showTips', {
              text: res.data.message,
              color: 'red',
            });
          }
        })
        .catch((error) => {
          console.log(error);
        });
    },
  },
};
</script>
