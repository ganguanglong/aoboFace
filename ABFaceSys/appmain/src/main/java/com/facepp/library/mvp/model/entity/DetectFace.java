package com.facepp.library.mvp.model.entity;

import java.util.List;

/**
 * 脸：image_id（用于区分每一次请求的唯一的字符串。）
 *      ,request_id（被检测的图片在系统中的标识。）
 *      ，time_used（用时）
 *      ，脸部集合
 * Created by Administrator on 2017/9/21 0021.
 * image_id，request_id，time_used，faces，
 */

public class DetectFace {

    /**
     * image_id : UrEMr8mak66s53fpNQ0f3A==
     * request_id : 1505974826,ee687f83-4341-4c4a-96cf-3bb5db3af451
     * time_used : 407
     * faces : [{"attributes":{"gender":{"value":"Male"},"age":{"value":59}},"face_rectangle":{"width":79,"top":66,"left":74,"height":79},"face_token":"0d427ae61e574588c085448612953435"}]
     */

    private String image_id;
    private String request_id;
    private int time_used;

    private List<FacesBean> faces;

    public String getImage_id() {
        return image_id;
    }

    public void setImage_id(String image_id) {
        this.image_id = image_id;
    }

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public int getTime_used() {
        return time_used;
    }

    public void setTime_used(int time_used) {
        this.time_used = time_used;
    }

    public List<FacesBean> getFaces() {
        return faces;
    }

    public void setFaces(List<FacesBean> faces) {
        this.faces = faces;
    }

    /**
     * 脸部集合类：脸部属性，脸部矩形，face_token
     * attributes，face_rectangle，face_token，
     * class AttributesBean
     * class FaceRectangleBean
     */
    public static class FacesBean {
        /**
         * attributes : {"gender":{"value":"Male"},"age":{"value":59}}
         * face_rectangle : {"width":79,"top":66,"left":74,"height":79}
         * face_token : 0d427ae61e574588c085448612953435
         */

        private AttributesBean attributes;
        private FaceRectangleBean face_rectangle;
        private String face_token;

        public AttributesBean getAttributes() {
            return attributes;
        }

        public void setAttributes(AttributesBean attributes) {
            this.attributes = attributes;
        }

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

        /**
         * 返回属性类：性别，年龄
         * class GenderBean
         * class AgeBean
         */
        public static class AttributesBean {
            /**
             * gender : {"value":"Male"}
             * age : {"value":59}
             */

            private GenderBean gender;
            private AgeBean age;

            public GenderBean getGender() {
                return gender;
            }

            public void setGender(GenderBean gender) {
                this.gender = gender;
            }

            public AgeBean getAge() {
                return age;
            }

            public void setAge(AgeBean age) {
                this.age = age;
            }

            /**
             * 性别
             * value
             */
            public static class GenderBean {
                /**
                 * value : Male
                 */

                private String value;

                public String getValue() {
                    return value;
                }

                public void setValue(String value) {
                    this.value = value;
                }
            }

            /**
             * 年龄
             * value
             */
            public static class AgeBean {
                /**
                 * value : 59
                 */

                private int value;

                public int getValue() {
                    return value;
                }

                public void setValue(int value) {
                    this.value = value;
                }
            }
        }

        /**
         * 脸部矩形的宽高位置
         * width，top，left，height
         */
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
}
