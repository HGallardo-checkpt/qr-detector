

import axios from "axios";

const retrieveFullBaseURL = () => process.env.BASE_URL + "rest/example/";

const httpClient = axios.create({
  baseURL: retrieveFullBaseURL(),
  timeout: 5000,
  headers: {
    "Content-type": "application/json",
  },
});

export default httpClient;
