<template>
  <v-app id="rank">
    <v-container style="align-self: auto; text-align: center">

      <v-chip-group
          active-class="primary--text"
          column
      >
        <v-chip
            :key="index"
            v-for="(genre,index) in genres"
            @click="onGenreSearch(genre)"
        >
          {{ genre }}
        </v-chip>
      </v-chip-group>

      <h1 class="my-16">{{ curGenre }}</h1>
      <v-row>
        <v-col :key="i" v-for="(movie,i) in movies" cols="4">
          <MiscRating
              color="#f4f7f7"
              :movie="movie"
              :father-method="getNewRecs"
          ></MiscRating>
        </v-col>
      </v-row>
    </v-container>
  </v-app>
</template>

<script>
import MiscRating from "../components/MiscRating";
import {getGenresMovies, getTopAllMovies,rateToMovie} from "@/api/movie"

export default {
  name: "Rank",
  components: {MiscRating},
  created() {
    this.onGenreSearch(this.curGenre)
  },
  data: () => ({
    genres: ['Western', 'War', 'Comedy', 'Crime',  'Action', 'Fantasy', 'Romance', 'Mystery', 'Animation', 'Adventure', 'Drama', 'Documentary', 'Horror', 'Thriller'],
    curGenre: 'Western',
    movies: [{
      name: '',
      directors: '',
      score: 5.0,
      shoot: ''
    }],
    movieCnt: 9
  }),
  methods: {
    getTopMovies(num){
      getTopAllMovies({
        num: num
      }).then(response => {
        this.movies = response.data.movies
      }).catch(err => {
        console.log(err)
      })
    },
    onGenreSearch(genre) {
      this.curGenre = genre
      if (genre !== "All") {
        getGenresMovies({
          category: genre,
          num: this.movieCnt
        })
            .then(response => {
              this.movies = response.data.movies
            })
            .catch(error => {
                  console.log(error)
                }
            )
      } else {
        this.getTopMovies(this.movieCnt)
      }
    },
    getNewRecs(movie, newVal) {
      // if (this.first) {
      //     this.first = false;
      //     return
      // }
      let username = this.$store.state.username
      if (username == null || username === '') {
        username = "1@qq.com"
      }
      rateToMovie(movie.mid, newVal * 2, username)
      .then(() => {
      })
    },

  },
};
</script>

<style scoped>
</style>
