package com.codeabovelab.dm.platform.util;

/*
public class RedisJacksonSerializerTest {

    public static class Composite {
        private String str;
        private Composite comp;
        private List<String> strings;

        public Composite() {
        }

        public Composite(String str, Composite comp, String ... strings) {
            this.str = str;
            this.comp = comp;
            this.strings = Arrays.asList(strings);
        }

        public String getStr() {
            return str;
        }

        public void setStr(String str) {
            this.str = str;
        }

        public Composite getComp() {
            return comp;
        }

        public void setComp(Composite comp) {
            this.comp = comp;
        }

        public List<String> getStrings() {
            return strings;
        }

        public void setStrings(List<String> strings) {
            this.strings = strings;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Composite)) {
                return false;
            }

            Composite composite = (Composite) o;

            if (comp != null ? !comp.equals(composite.comp) : composite.comp != null) {
                return false;
            }
            if (str != null ? !str.equals(composite.str) : composite.str != null) {
                return false;
            }
            if (strings != null ? !strings.equals(composite.strings) : composite.strings != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = str != null ? str.hashCode() : 0;
            result = 31 * result + (comp != null ? comp.hashCode() : 0);
            result = 31 * result + (strings != null ? strings.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Composite{" +
              "str='" + str + '\'' +
              ", comp=" + comp +
              ", strings=" + strings +
              '}';
        }
    }

    @Test
    public void test()  {
        testMapper(new RedisJacksonSerializer.TypeAsStringMapper());
        Class<?>[] types = new Class[0xff];
        types[0] = String.class;
        types[1] = Integer.class;
        types[2] = Long.class;
        types[0xff - 1] = Composite.class;
        testMapper(new RedisJacksonSerializer.TypeAsIndexMapper(types));
    }

    private void testMapper(RedisJacksonSerializer.TypeMapper mapper) {
        RedisJacksonSerializer rjs = new RedisJacksonSerializer(new ObjectMapper(), mapper);
        tetsObject(rjs, "1");
        tetsObject(rjs, 1);
        tetsObject(rjs, 1l);
        tetsObject(rjs, new Composite("test", new Composite("child", null, "c1", "c2")));
    }

    private void tetsObject(RedisJacksonSerializer rjs, Object o) {
        byte[] bytes = rjs.serialize(o);
        Object res = rjs.deserialize(bytes);
        Assert.assertEquals(o, res);
    }

}*/