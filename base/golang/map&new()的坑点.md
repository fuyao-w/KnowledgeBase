## 代码1：

```go
func TestMap(t *testing.T) {
   m := new(map[string]interface{})
  t.Log(m)
   t.Log(json.Unmarshal([]byte(`{"error":0}`), m))
	if code, ok := (*m)["error"].(int); !ok || code != 0 {
		t.Log(ok)
		t.Log(code)
	}

}
```

### 问题1 ：这段程序会打印出什么？

结果：

```
&map[]  // 注意
<nil>
false
0
```

### 问题2 ：这段程序有哪些点需要注意？

1. `new()`函数返回了什么? m 具体是什么？

   答:（go doc） `new()`函数返回了，指向指定类型零值的指针。m 是一个指针，这个指针不为空。但它指向一个零值的`map`。*map 相关见代码2 引出的问题*。

2. 疑问：除了 `new()` 函数，有没有其他方法创造出上述 `m（见打印结果 &map[]）`这样的 `map`）?

   答: 目前通过正常途径无法做到。

3. 疑问：`Unmarshal`为什么可以正常处理这样的 `map`？

   答：（go doc 翻译）要将JSON对象`Unmarshal`到`map`中，`Unmarshal`首先要建立要使用的`map`。如果`map`为零值，`Unmarshal`将分配一个新`map`。否则，`Unmarshal`会重用现有地图，并保留现有条目。然后，`Unmarshal`将来自JSON对象的键/值对存储到映射中。`map`的键类型必须是字符串，整数或实现`encoding.TextUnmarshaler`。

---



5. 疑问：为什么 ok == false ?

答：`error` 值实际类型为 `float64`，运行如下代码。`Unmarshal` 将数字 `0` 解析为 `float64` 类型：

```go
	m := new(map[string]interface{})
	t.Log(json.Unmarshal([]byte(`{"dm_error":0}`), m))
	if code, ok := (*m)["dm_error"]; !ok || code != 0 {
		t.Log(ok)
		t.Log(reflect.TypeOf(code))
	}
```

6. `if code, ok := (*m)["dm_error"]; !ok || code != 0 {`  这行中 `code` 作为 `interface{}`类型可以直接与具体类型的值比？

   答：当然可以这么做，只是平时的编码习惯将其断言为具体类型。但是需要注意`interface{}`的具体类型是什么,就像上面遇到的问题那样？

7. `if code, ok := (*m)["dm_error"].(float64); !ok || code != 0` 这样断言成功，并且`code != 0 `返回`flase` 为什么？

   答：（来自Go 语言程序设计）无类型的数值常量可以兼容表达式中任何（内置的）类型额数值，因此我们可以直接将一个无类型的数值常量与另一个数值做加法，或者将一个无类型的常量与另一个数值进行比较，无论另一个数值是什么类型。

______

## 代码2：

```go
var m *map[string]int
t.Log(m)
t.Log(json.Unmarshal([]byte(`{"code":0}`), &m))
t.Log(m)
```

### 问题：这段代码会打印出什么？

    map_test.go:34: <nil>
    map_test.go:37: <nil>
    map_test.go:38: &map[code:0]

疑问：为啥没崩溃？

答：首先需要大致了解 `go`中`map` 的底层是什么？ -> 是一个指针指向底层的`map结构`。

通过 var 关键字声明的字段都会初始化零值。第二行打印出的 `nil`不代表引用`m`是一个空指针，`m`是一个有效的指针(打印`&m（打印地址）` 和`*m（崩溃）`可见)，但是它指向的`map`是零值。 



----

以上问题需要思考的点：

1. 指针的使用

2. `new()` 创建的 `map` 与用其他方式创建的 `map`有何不同 

3. `Unmarshal`& map 指针 

4. `Unmarshal`& int 值

5. `interface{}` 可以与基础类型直接比较  

6. 数字常量与执行类型数字如何进行比较

   