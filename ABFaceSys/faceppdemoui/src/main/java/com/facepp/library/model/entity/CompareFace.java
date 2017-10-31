package com.facepp.library.model.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 项目名：   ABFaceSys
 * 包名：     com.facepp.library.model.entity
 * 创建者：   Arsenalong
 * 创建时间： 2017/10/27    17:27
 * 描述：
 */

public class CompareFace {


    /**
     * faces1 : [{"face_rectangle":{"width":79,"top":66,"left":74,"height":79},"face_token":"f2705fd633d912fd8f670c1112568eaf"}]
     * faces2 : [{"face_rectangle":{"width":148,"top":103,"left":69,"height":148},"face_token":"a87fdaaef91cac9c303b2d49a00f0838"}]
     * time_used : 624
     * thresholds : {"1e-3":62.327,"1e-5":73.975,"1e-4":69.101}
     * confidence : 17.334
     * image_id2 : qBgSnnGCPpfqj+1+roreoA==
     * image_id1 : UrEMr8mak66s53fpNQ0f3A==
     * request_id : 1509096357,3c4b87a3-4073-471e-8ea0-b0df5fe2be59
     */

    /**
     * "faces1": [{"face_rectangle": {"width": 225, "top": 199, "left": 41, "height": 225}, "face_token": "79a9b41627110f6ac362501ce7b5726b"}]
     * "faces2": [{"face_rectangle": {"width": 235, "top": 224, "left": 49, "height": 235}, "face_token": "bcbf1cd3237961ff22b284bd14015a7b"}]
     * "time_used": 628"
     * "thresholds": {"1e-3": 62.327, "1e-5": 73.975, "1e-4": 69.101}
     * "confidence": 95.957
     * "image_id2": "JfbvZa9MQf+vGTm8FWV/gQ=="
     * "image_id1": "P53JhOF4s/H3OgCHvuZbpQ=="
     * "request_id": "1509102202,77d1f172-6bf7-432f-9df0-451a2acd0f53"}
     */
    private int time_used;
    private ThresholdsBean thresholds;
    private double confidence;
    private String image_id2;
    private String image_id1;
    private String request_id;
    private List<Faces1Bean> faces1;
    private List<Faces2Bean> faces2;

    public int getTime_used() {
        return time_used;
    }

    public void setTime_used(int time_used) {
        this.time_used = time_used;
    }

    public ThresholdsBean getThresholds() {
        return thresholds;
    }

    public void setThresholds(ThresholdsBean thresholds) {
        this.thresholds = thresholds;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getImage_id2() {
        return image_id2;
    }

    public void setImage_id2(String image_id2) {
        this.image_id2 = image_id2;
    }

    public String getImage_id1() {
        return image_id1;
    }

    public void setImage_id1(String image_id1) {
        this.image_id1 = image_id1;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public List<Faces1Bean> getFaces1() {
        return faces1;
    }

    public void setFaces1(List<Faces1Bean> faces1) {
        this.faces1 = faces1;
    }

    public List<Faces2Bean> getFaces2() {
        return faces2;
    }

    public void setFaces2(List<Faces2Bean> faces2) {
        this.faces2 = faces2;
    }

    public static class ThresholdsBean {
        /**
         * 1e-3 : 62.327
         * 1e-5 : 73.975
         * 1e-4 : 69.101
         */

        @SerializedName("1e-3")
        private double _$1e3;
        @SerializedName("1e-5")
        private double _$1e5;
        @SerializedName("1e-4")
        private double _$1e4;

        public double get_$1e3() {
            return _$1e3;
        }

        public void set_$1e3(double _$1e3) {
            this._$1e3 = _$1e3;
        }

        public double get_$1e5() {
            return _$1e5;
        }

        public void set_$1e5(double _$1e5) {
            this._$1e5 = _$1e5;
        }

        public double get_$1e4() {
            return _$1e4;
        }

        public void set_$1e4(double _$1e4) {
            this._$1e4 = _$1e4;
        }
    }

    public static class Faces1Bean {
        /**
         * face_rectangle : {"width":79,"top":66,"left":74,"height":79}
         * face_token : f2705fd633d912fd8f670c1112568eaf
         */

        private FaceRectangleBean face_rectangle;
        private String face_token;

        public FaceRectangleBean getFace_rectangle() {
            return face_rectangle;
        }

        public void setFace_rectangle(FaceRectangleBean face_rectangle) {
            this.face_rectangle = face_rectangle;
        }

        public String getFace_token() {
            return face_token;
        }

        public void setFace_token(String face_token) {
            this.face_token = face_token;
        }

        public static class FaceRectangleBean {
            /**
             * width : 79
             * top : 66
             * left : 74
             * height : 79
             */

            private int width;
            private int top;
            private int left;
            private int height;

            public int getWidth() {
                return width;
            }

            public void setWidth(int width) {
                this.width = width;
            }

            public int getTop() {
                return top;
            }

            public void setTop(int top) {
                this.top = top;
            }

            public int getLeft() {
                return left;
            }

            public void setLeft(int left) {
                this.left = left;
            }

            public int getHeight() {
                return height;
            }

            public void setHeight(int height) {
                this.height = height;
            }
        }
    }

    public static class Faces2Bean {
        /**
         * face_rectangle : {"width":148,"top":103,"left":69,"height":148}
         * face_token : a87fdaaef91cac9c303b2d49a00f0838
         */

        private FaceRectangleBeanX face_rectangle;
        private String face_token;

        public FaceRectangleBeanX getFace_rectangle() {
            return face_rectangle;
        }

        public void setFace_rectangle(FaceRectangleBeanX face_rectangle) {
            this.face_rectangle = face_rectangle;
        }

        public String getFace_token() {
            return face_token;
        }

        public void setFace_token(String face_token) {
            this.face_token = face_token;
        }

        public static class FaceRectangleBeanX {
            /**
             * width : 148
             * top : 103
             * left : 69
             * height : 148
             */

            private int width;
            private int top;
            private int left;
            private int height;

            public int getWidth() {
                return width;
            }

            public void setWidth(int width) {
                this.width = width;
            }

            public int getTop() {
                return top;
            }

            public void setTop(int top) {
                this.top = top;
            }

            public int getLeft() {
                return left;
            }

            public void setLeft(int left) {
                this.left = left;
            }

            public int getHeight() {
                return height;
            }

            public void setHeight(int height) {
                this.height = height;
            }
        }
    }
}
