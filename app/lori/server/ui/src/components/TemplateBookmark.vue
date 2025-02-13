<script lang="ts">
import { computed, defineComponent, onMounted, Ref, ref, watch } from "vue";
import bookmarkApi from "@/api/bookmarkApi";
import { BookmarkRest } from "@/generated-sources/openapi";
import error from "@/utils/error";

export default defineComponent({
  props: {
    reinitCounter: {
      type: Number,
      required: false,
    },
  },
  emits: ["bookmarksSelected", "templateBookmarkClosed"],
  setup(props, { emit }) {
    const headers = [
      {
        title: "Name",
        align: "start",
        value: "bookmarkName",
        sortable: true,
      },
      {
        title: "Id",
        value: "bookmarkId",
        sortable: true,
      },
    ];

    const bookmarkItems: Ref<Array<BookmarkRest>> = ref([]);
    const searchTerm = ref("");
    const selectedBookmarks = ref([]);
    const getBookmarkList = () => {
      bookmarkApi
        .getBookmarkList(0, 100) // TODO: simplification for now
        .then((r: Array<BookmarkRest>) => {
          bookmarkItems.value = r;
        })
        .catch((e) => {
          error.errorHandling(e, (errMsg: string) => {
            errorMsg.value = errMsg;
            errorMsgIsActive.value = true;
          });
        });
    };

    /**
     * On Close & Save
     */
    const close = () => {
      emit("templateBookmarkClosed");
    };

    const save = () => {
      emit("bookmarksSelected", selectedBookmarks.value);
      close();
    };

    /**
     * Error messages.
     */
    const errorMsgIsActive = ref(false);
    const errorMsg = ref("");

    onMounted(() => getBookmarkList());
    const computedReinitCounter = computed(() => props.reinitCounter);
    watch(computedReinitCounter, () => {
      // Actions executed when the window is displayed:
      selectedBookmarks.value = [];
      getBookmarkList();
    });

    return {
      headers,
      bookmarkItems,
      errorMsgIsActive,
      errorMsg,
      searchTerm,
      selectedBookmarks,
      close,
      save,
      getBookmarkList,
    };
  },
});
</script>

<style scoped></style>
<template>
  <v-card position="relative">
    <v-container>
      <v-card-title>Auswahl Gespeicherte Suche</v-card-title>
      <v-snackbar
          contained
          multi-line
          location="top"
          timer="true"
          timeout="5000"
          v-model="errorMsgIsActive"
          color="error"
      >
        {{ errorMsg }}
      </v-snackbar>
      <v-text-field
        v-model="searchTerm"
        append-icon="mdi-magnify"
        hide-details
        label="Suche"
        single-line
      ></v-text-field>
      <v-data-table
        v-model="selectedBookmarks"
        :headers="headers"
        :items="bookmarkItems"
        :search="searchTerm"
        item-value="bookmarkId"
        show-select
        return-object
      >
      </v-data-table>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn color="blue darken-1" text="Zurück" @click="close"></v-btn>
        <v-btn
          :disabled="selectedBookmarks.length == 0"
          color="blue darken-1"
          text="Speichern"
          @click="save"
        >
        </v-btn>
      </v-card-actions>
    </v-container>
  </v-card>
</template>
