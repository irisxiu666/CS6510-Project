<template>
  <v-app id="rank">
    <v-container style="align-self: auto; text-align: center">
      <v-chip-group active-class="primary--text" column>
        <v-chip
          :key="index"
          @click="onChaneType(type)"
          v-for="(type, index) in types"
        >
          {{ type }}
        </v-chip>
      </v-chip-group>
      <h1 class="my-16">{{ this.curType }}</h1>

      <v-row>
        <v-col cols="4" :key="i" v-for="(movie, i) in movies">
          <MiscRating
            color="#f4f7f7"
            :father-method="getNewRecs"
            :movie="movie"
          ></MiscRating>
        </v-col>
      </v-row>
    </v-container>

    <v-dialog v-model="dialog" persistent max-width="550">
      <v-card>
        <v-card-title>Choose your favourite</v-card-title>
        <v-container class="genre-selector">
          <v-checkbox
            :key="index"
            v-for="(genre, index) in genres"
            v-model="selected"
            :label="genre"
            :value="genre"
          ></v-checkbox>
        </v-container>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn color="green darken-1" text @click="addPref">确认</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-app>
</template>

<script>
import MiscRating from '../components/MiscRating';
import {
  getHistoryHotMovies,
  getRecentlyHotMovies,
  getAvgHotMovies,
  rateToMovie,
} from '@/api/movie';
import { addPrefGenres } from '@/api/user';

export default {
  name: 'Rank',
  data() {
    return {
      dialog: false,
      genres: [
        'Western',
        'War',
        'Comedy',
        'Crime',
        'Music',
        'Action',
        'Fantasy',
        'Romance',
        'Mystery',
        'Animation',
        'Adventure',
        'Drama',
        'Documentary',
        'Horror',
        'Thriller',
      ],
      selected: ['Western'],
      movies: [
        {
          name: '',
          directors: '',
          score: 5.0,
          shoot: '',
        },
      ],
      types: ['Recently hot', 'Historical hot', 'Highest rating'],
      curType: 'Recently hot',
      movieCnt: 9,
      func: {
        recentFunc: getRecentlyHotMovies,
        historyFunc: getHistoryHotMovies,
        avgFunc: getAvgHotMovies,
      },
    };
  },
  methods: {
    getFunc() {
      let map = {
        'Recently hot': this.func.recentFunc,
        'Historical hot': this.func.historyFunc,
        'Highest rating': this.func.avgFunc,
      };
      return map[this.curType];
    },
    onChaneType(type) {
      this.curType = type;
      if (type === 'Recently hot') {
        // Recently hot
        this.getMovies(this.func.recentFunc, this.movieCnt);
      } else if (type === 'Historical hot') {
        // Historical hot
        this.getMovies(this.func.historyFunc, this.movieCnt);
      } else {
        this.getMovies(this.func.avgFunc, this.movieCnt);
      }
    },
    getMovies(func, num) {
      func({ num: num })
        .then((response) => {
          this.movies = response.data.movies;
        })
        .catch();
    },

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

    addPref() {
      this.dialog = false;
      addPrefGenres({
        username: this.$store.state.username,
        genres: this.preference,
      })
        .then((response) => {
          if (response.data.success) {
            this.$store.commit('showTips', {
              text: 'Successfully added favourite',
            });
            //this.getMovies()
          }
        })
        .catch((error) => {
          this.$store.commit('showTips', { text: error, color: 'red' });
        });
    },
  },
  computed: {
    preference: {
      get: function() {
        let selected = this.selected;
        let result = '';
        for (let i = 0; i < selected.length; i++) {
          result = result.concat(selected[i]);
          if (i !== selected.length - 1) result = result.concat(',');
        }
        return result;
      },
    },
  },
  created() {
    this.getMovies(this.func.recentFunc, this.movieCnt);
    // checkNew({
    //   username: this.$store.state.username
    // }).then(response => {
    //   if (response.data.new) {
    //     this.dialog = true
    //   }
    // })
    if (this.$store.state.prefGenres.length <= 0) {
      this.dialog = true;
      this.$store.commit('setPrefGenres', ['abc']);
    }
  },
  components: { MiscRating },
};
</script>

<style scoped>
.genre-selector {
  padding: 10px 40px;
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: space-around;
}

.genre-selector > div {
  width: 33%;
}
</style>
