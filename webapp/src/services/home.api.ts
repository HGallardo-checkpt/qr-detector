

import { QRDetectorReaderDTO } from "@/interfaces";
import { AxiosResponse } from "axios";
import httpClient from "./httpClient";

const getQRDetectionStatistics = (): Promise<AxiosResponse<QRDetectorReaderDTO>> =>
  httpClient.get<QRDetectorReaderDTO>("qrdetection-statistics");

export { getQRDetectionStatistics };
