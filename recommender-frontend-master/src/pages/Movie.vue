<template>
  <v-app>
    <h1 class="my-16">{{ movie.name }}</h1>
    <v-card width="80%" color="#f4f7f7" class="mx-auto elevation-2 mt-8" dark>
      <v-row>
        <v-col>
          <v-card-title>
            <div style="color: #202124">
              <!--              <div class="headline">-->
              <!--            {{ movie.name }}    -->
              <!--              </div>-->
              <div>Director：{{ movie.directors }}</div>
              <div>Starring：{{ primaryActors }}</div>
              <div>Type：{{ movie.genres }}</div>
              <div>Language: {{ movie.language }}</div>
              <div>Duration: {{ movie.timelong }}</div>
              <div>Release date：{{ movie.shoot }}</div>
              <div>Abstract：{{ movie.descri }}</div>
            </div>
          </v-card-title>
        </v-col>
        <v-img
          :src="movie.image"
          class="shrink ma-2"
          contain
          height="125px"
          style="flex-basis: 125px"
        ></v-img>
      </v-row>
      <v-divider dark></v-divider>
      <v-card-actions class="pa-4" style="color: #202124">
        <v-spacer></v-spacer>
        <span class="black--text text--lighten-2 caption mr-2">
          ({{ movie.score }})
        </span>
        <v-rating
          background-color="#737373"
          color="yellow accent-4"
          dense
          half-increments
          readonly
          hover
          size="18"
          v-model="movie.score"
        ></v-rating>
      </v-card-actions>
    </v-card>
    <v-container style="align-self: auto; text-align: center">
      <h1 class="my-16">Similar movies</h1>

      <v-row>
        <v-col cols="4" :key="i" v-for="(movie, i) in similarMovies">
          <MiscRating
            color="#f4f7f7"
            :father-method="getNewRecs"
            :movie="movie"
          ></MiscRating>
        </v-col>
      </v-row>
    </v-container>
  </v-app>
</template>

<script>
import { getMovieInfo, getSimilarMovies, rateToMovie } from '@/api/movie';
import MiscRating from '@/components/MiscRating';

export default {
  name: 'Movie',
  components: { MiscRating },
  data: () => ({
    movie: {
      timelong: '',
      descri: '',
      actors: '',
    },
    similarMovies: [],
  }),
  methods: {
    getNewRecs(movie, newVal) {
      // if (this.first) {
      //     this.first = false;
      //     return
      // }
      let username = this.$store.state.username;
      if (username == null || username === '') {
        username = '1@qq.com';
      }
      rateToMovie(movie.mid, newVal * 2, username).then(() => {});
    },
  },
  created() {
    getMovieInfo(this.$route.params.mid)
      .then((res) => {
        this.movie = res.data.movie;
      })
      .catch((err) => console.log(err));
  },

  watch: {
    movie() {
      this.$nextTick(() => {
        getSimilarMovies(this.movie.mid, { num: 9 })
          .then((response) => {
            this.similarMovies = response.data.movies;
          })
          .catch((error) => {
            console.log(error);
          });
      });
    },
  },
  computed: {
    primaryActors() {
      let actors = this.movie.actors.split('|');
      let primary = '';
      for (let i = 0; i < actors.length; i++) {
        if (i === 3) {
          break;
        }
        primary += actors[i] + ' | ';
      }
      return primary.substr(0, primary.length - 2);
    },
  },
};
</script>

<style scoped></style>
