<template>
  <div class="admin-tools">
    <div class="page-header">
      <div class="header-left">
        <h2>工具管理</h2>
        <el-tag type="info" size="large">总计 {{ total }} 个工具</el-tag>
      </div>
      <el-button type="primary" size="large" @click="handleAdd" class="add-btn">
        <el-icon><Plus /></el-icon>
        新增工具
      </el-button>
    </div>

    <el-card class="table-card" shadow="never">

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="工具名称" />
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="currentVersion" label="版本" width="100" />
        <el-table-column prop="owner" label="负责人" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="工具名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="工具描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="工具类型" prop="type">
          <el-select v-model="form.type" style="width: 100%" placeholder="请选择类型">
            <el-option label="平台" value="平台" />
            <el-option label="工具" value="工具" />
            <el-option label="系统" value="系统" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="工具图标" prop="iconUrl">
          <div class="icon-upload-container">
            <el-upload
              class="icon-uploader"
              :action="''"
              :show-file-list="false"
              :before-upload="beforeIconUpload"
              :http-request="handleIconUpload"
              accept="image/jpeg,image/jpg,image/png,image/gif,image/svg+xml"
            >
              <img 
                v-if="form.iconUrl" 
                :src="iconPreviewCache.get(form.iconUrl) || form.iconUrl" 
                class="icon-preview"
                @error="handleImageError"
                @load="handleImageLoad" 
              />
              <el-icon v-else class="icon-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <div class="icon-tips">
              <div>支持 JPG、PNG、GIF、SVG 格式</div>
              <div>建议尺寸：128x128，大小不超过2MB</div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="访问地址" prop="accessUrl">
          <el-input v-model="form.accessUrl" />
        </el-form-item>
        <el-form-item label="当前版本" prop="currentVersion">
          <el-input v-model="form.currentVersion" />
        </el-form-item>
        <el-form-item label="负责人" prop="owner">
          <el-input v-model="form.owner" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="已下线" :value="0" />
            <el-option label="运行中" :value="1" />
            <el-option label="维护中" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getToolPage, addTool, updateTool, deleteTool } from '@/api/tool'
import { uploadIcon } from '@/api/upload'
import type { Tool } from '@/types'

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增工具')
const formRef = ref<FormInstance>()

// 图标预览缓存（使用 Map 存储 URL -> base64）
const iconPreviewCache = new Map<string, string>()

const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const tableData = ref<Tool[]>([])

const form = reactive<Tool>({
  name: '',
  description: '',
  type: '',
  iconUrl: '',
  accessUrl: '',
  currentVersion: '',
  owner: '',
  status: 1,
  sortOrder: 0
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入工具名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getToolPage({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    if (res.data) {
      tableData.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

const getStatusType = (status?: number) => {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'warning'
    default: return ''
  }
}

const getStatusText = (status?: number) => {
  switch (status) {
    case 0: return '已下线'
    case 1: return '运行中'
    case 2: return '维护中'
    default: return '未知'
  }
}

const resetForm = () => {
  Object.assign(form, {
    id: undefined,
    name: '',
    description: '',
    type: '',
    iconUrl: '',
    accessUrl: '',
    currentVersion: '',
    owner: '',
    status: 1,
    sortOrder: 0
  })
  formRef.value?.clearValidate()
}

const handleAdd = () => {
  resetForm()
  dialogTitle.value = '新增工具'
  dialogVisible.value = true
}

const handleEdit = (row: Tool) => {
  Object.assign(form, row)
  
  // 处理图标URL格式
  if (form.iconUrl) {
    if (form.iconUrl.startsWith('http://') || form.iconUrl.startsWith('https://')) {
      try {
        form.iconUrl = new URL(form.iconUrl).pathname
      } catch (e) {
        console.error('解析图标URL失败:', form.iconUrl)
      }
    } else if (!form.iconUrl.startsWith('/api/icon/') && !form.iconUrl.startsWith('/files/')) {
      form.iconUrl = `/api/icon/${form.iconUrl}`
    }
  }
  
  dialogTitle.value = '编辑工具'
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      if (form.id) {
        await updateTool(form.id, form)
        ElMessage.success('更新成功')
      } else {
        await addTool(form)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (error) {
      console.error('提交失败', error)
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = (row: Tool) => {
  ElMessageBox.confirm('确定要删除这个工具吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteTool(row.id!)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      console.error('删除失败', error)
    }
  })
}

// 图标上传前验证
const beforeIconUpload = (file: File) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过2MB！')
    return false
  }
  return true
}

const handleIconUpload = async (options: UploadRequestOptions) => {
  const file = options.file as File
  try {
    const category = form.type || 'default'
    const res = await uploadIcon(file, category)
    if (res.data) {
      form.iconUrl = `/api/icon/${res.data.relativePath}`
      
      // 缓存base64用于即时预览
      const reader = new FileReader()
      reader.onload = (e) => {
        if (e.target?.result) {
          iconPreviewCache.set(form.iconUrl, e.target.result as string)
        }
      }
      reader.readAsDataURL(file)
      
      ElMessage.success('图标上传成功')
    }
  } catch (error) {
    console.error('图标上传失败', error)
    ElMessage.error('图标上传失败')
  }
}

// 获取完整的图标URL
const getFullIconUrl = (url?: string) => {
  if (!url) return ''
  // 如果是完整URL，直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  // 如果是相对路径，转换为API路径
  // /files/xxx.png -> /api/files/icon/xxx.png
  if (url.startsWith('/files/')) {
    const filename = url.substring(7) // 去掉 '/files/'
    return `http://localhost:8080/api/files/icon/${filename}`
  }
  // 直接是文件名
  return `http://localhost:8080/api/files/icon/${url}`
}

const handleImageLoad = () => {
  // 图标加载成功
}

const handleImageError = () => {
  ElMessage.error('图标加载失败')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.admin-tools {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: var(--card-bg);
  border-radius: 12px;
  border: 2px solid var(--card-border);
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--color-primary);
}

.add-btn {
  padding: 12px 24px;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(255,140,0,0.3);
}

.add-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(255,140,0,0.4);
}

.table-card {
  background: var(--card-bg) !important;
  border: 2px solid var(--card-border) !important;
  border-radius: 12px !important;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.icon-upload-container {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.icon-uploader {
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.icon-uploader:hover {
  border-color: #ff8c00;
}

.icon-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 128px;
  height: 128px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-preview {
  width: 128px;
  height: 128px;
  display: block;
  object-fit: cover;
}

.icon-tips {
  flex: 1;
  color: #909399;
  font-size: 12px;
  line-height: 1.8;
}
</style>
