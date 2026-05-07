export function normalizePluginFunctions(pluginList: any[]) {
  const result = []
  for (let index = 0; index < pluginList.length; index += 1) {
    result.push(normalizePluginFunction(pluginList[index]))
  }
  return result
}

export function normalizePluginFunction(item: any) {
  const meta = parsePluginFields(item.fields)
  const params: Record<string, any> = {}

  for (let index = 0; index < meta.length; index += 1) {
    const fieldName = getPluginFieldName(meta[index])
    if (fieldName) {
      Object.defineProperty(params, fieldName, {
        value: getPluginFieldDefault(meta[index]),
        enumerable: true,
        configurable: true,
        writable: true,
      })
    }
  }

  return { ...item, fieldsMeta: meta, params }
}

export function parsePluginFields(fields: any) {
  if (Array.isArray(fields))
    return fields
  if (!fields)
    return []
  if (typeof fields === 'object') {
    const values = Object.values(fields)
    return values.every(item => item && typeof item === 'object') ? values : []
  }
  if (typeof fields !== 'string') {
    console.error('插件字段类型异常:', fields)
    return []
  }

  try {
    const parsed = JSON.parse(fields)
    if (Array.isArray(parsed))
      return parsed
    console.error('插件字段不是数组:', parsed)
    return []
  }
  catch (error) {
    console.error('解析插件字段失败:', fields, error)
    return []
  }
}

export function getPluginFieldProp(field: any, prop: string) {
  return field?.[prop]
}

export function getPluginFieldName(field: any) {
  return String(getPluginFieldProp(field, 'key') || '')
}

export function getPluginFieldType(field: any) {
  return String(getPluginFieldProp(field, 'type') || '')
}

export function getPluginFieldLabel(field: any) {
  return String(getPluginFieldProp(field, 'label') || '')
}

export function getPluginFieldDefault(field: any) {
  return getPluginFieldProp(field, 'default')
}
