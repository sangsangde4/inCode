<template>
  <div class="admin-files">
    <div class="page-header">
      <div class="header-left">
        <h2>文件管理</h2>
        <el-tag type="info" size="large">总计 {{ tableData.length }} 个文件</el-tag>
      </div>
      <el-button type="primary" size="large" @click="dialogVisible = true" class="upload-btn">
        <el-icon><Upload /></el-icon>
        上传文件
      </el-button>
    </div>

    <el-card class="table-card" shadow="never">
      <FileBrowser
        :files="tableData"
        :loading="loading"
        :isAdmin="true"
        @download="handleDownload"
        @delete="handleDelete"
        @deleteFolder="handleDeleteFolder"
      />
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog v-model="dialogVisible" title="上传文件" width="600px" @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="margin-top: 20px;">
        <el-form-item label="关联工具" prop="toolId">
          <el-select v-model="form.toolId" style="width: 100%" filterable>
            <el-option
              v-for="tool in tools"
              :key="tool.id"
              :label="tool.name"
              :value="tool.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="form.version" placeholder="如：1.0.0（必填）" />
        </el-form-item>
        <el-form-item label="架构类型" prop="architecture">
          <el-select v-model="form.architecture" style="width: 100%" clearable placeholder="选择架构（可选）">
            <el-option label="Linux x64" value="linux_x64" />
            <el-option label="Linux ARM64" value="linux_arm64" />
            <el-option label="Windows x64" value="windows_x64" />
            <el-option label="Windows x86" value="windows_x86" />
            <el-option label="Windows ARM64" value="windows_arm64" />
            <el-option label="macOS x64 (Intel)" value="macos_x64" />
            <el-option label="macOS ARM64 (Apple Silicon)" value="macos_arm64" />
            <el-option label="通用 / 其他" value="universal" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件说明" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="选择文件" prop="files">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            multiple
            :limit="FILE_COUNT.MAX_FILES"
            :on-change="handleFileChange"
            :file-list="fileList"
            :on-remove="handleFileRemove"
            :on-exceed="handleExceed"
            :show-file-list="true"
            list-type="text"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                <div>📁 支持批量上传，最多一次选择10个文件</div>
                <div>📏 单个文件最大 <strong>2GB</strong></div>
                <div>📦 总大小最大 <strong>10GB</strong></div>
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item v-if="uploading">
          <el-progress
            type="line"
            :percentage="uploadProgress"
            :status="uploadProgress === 100 ? 'success' : undefined"
            :stroke-width="16"
            :show-text="true"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false" :disabled="uploading">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="uploading">
          {{ uploading ? '上传中...' : '上传' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile } from 'element-plus'
import {getFilePage, uploadFile, uploadFiles, deleteFile, deleteFolder} from '@/api/file'
import { getToolList } from '@/api/tool'
import { useUserStore } from '@/stores/user'
import { createVersionValidationRule } from '@/utils/semanticVersion'
import { FILE_SIZE, FILE_COUNT, formatFileSize } from '@/config/uploadLimits'
import FileBrowser from '@/components/FileBrowser.vue'
import type { ToolFile, Tool } from '@/types'

const userStore = useUserStore()
const loading = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const dialogVisible = ref(false)
const formRef = ref<FormInstance>()

const tableData = ref<ToolFile[]>([])
const tools = ref<Tool[]>([])
const fileList = ref<UploadFile[]>([])

const form = reactive({
  toolId: undefined as number | undefined,
  version: '',
  architecture: '',
  description: '',
  files: [] as File[]
})

const rules: FormRules = {
  toolId: [{ required: true, message: '请选择工具', trigger: 'change' }],
  version: [createVersionValidationRule(true)], // 版本号必填
  files: [{ required: true, message: '请选择文件', trigger: 'change' }]
}

const loadData = async () => {
  loading.value = true
  try {
    // 获取所有文件（不分页）
    const res = await getFilePage({
      pageNum: 1,
      pageSize: 10000 // 获取所有文件
    })
    if (res.data) {
      tableData.value = res.data.records
    }
  } catch (error) {
    console.error('加载数据失败', error)
  } finally {
    loading.value = false
  }
}

const loadTools = async () => {
  try {
    const res = await getToolList()
    if (res.data) {
      tools.value = res.data
    }
  } catch (error) {
    console.error('加载工具列表失败', error)
  }
}

const handleFileChange = (file: UploadFile, fileListArg?: UploadFile[]) => {
  // 同步 element-plus 的文件列表
  if (fileListArg) {
    fileList.value = fileListArg
  }
  // 收集原生 File 列表
  form.files = fileList.value
    .map(f => f.raw)
    .filter((f): f is File => !!f)

  // 验证文件大小
  validateFileSizes()
}

const validateFileSizes = () => {
  // 检查单个文件大小
  for (const file of form.files) {
    if (file.size > FILE_SIZE.MAX_FILE_SIZE) {
      const fileSize = formatFileSize(file.size)
      const maxSize = formatFileSize(FILE_SIZE.MAX_FILE_SIZE)
      ElMessage.error(`文件 "${file.name}" 大小为 ${fileSize}，超过限制！单个文件最大 ${maxSize}`)
      return false
    }
  }

  // 检查总大小
  const totalSize = form.files.reduce((sum, file) => sum + file.size, 0)
  if (totalSize > FILE_SIZE.MAX_TOTAL_SIZE) {
    const currentTotal = formatFileSize(totalSize)
    const maxTotal = formatFileSize(FILE_SIZE.MAX_TOTAL_SIZE)
    ElMessage.error(`所有文件总大小为 ${currentTotal}，超过限制！总大小最大 ${maxTotal}`)
    return false
  }

  return true
}

const handleFileRemove = (file: UploadFile, fileListArg: UploadFile[]) => {
  fileList.value = fileListArg
  form.files = fileList.value
    .map(f => f.raw)
    .filter((f): f is File => !!f)
}

const handleExceed = () => {
  ElMessage.warning(`一次最多选择 ${FILE_COUNT.MAX_FILES} 个文件`)
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    if (!form.files || form.files.length === 0) {
      ElMessage.error('请选择文件')
      return
    }

    // 验证文件大小
    if (!validateFileSizes()) {
      return
    }

    uploading.value = true
    try {
      const formData = new FormData()
      // 多文件字段名为 files，对应后端 @RequestParam("files")
      form.files.forEach(f => formData.append('files', f))
      formData.append('toolId', String(form.toolId))
      formData.append('version', form.version)
      if (form.architecture) {
        formData.append('architecture', form.architecture)
      }
      formData.append('description', form.description)
      formData.append('uploader', userStore.realName)

      // 批量上传并显示整体进度
      await uploadFiles(formData, (progressEvent: any) => {
        if (progressEvent.total) {
          uploadProgress.value = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
      })
      ElMessage.success('上传成功')
      dialogVisible.value = false
      resetForm()
      loadData()
    } catch (error) {
      console.error('上传失败', error)
    } finally {
      uploading.value = false
    }
  })
}

const resetForm = () => {
  form.toolId = undefined
  form.version = ''
  form.architecture = ''
  form.description = ''
  form.files = []
  fileList.value = []
  uploadProgress.value = 0
  formRef.value?.clearValidate()
}

const handleDialogClose = () => {
  resetForm()
}

const handleDownload = (row: ToolFile) => {
  // 优先使用路径下载URL
  if (row.downloadUrlByPath) {
    window.open(row.downloadUrlByPath, '_blank')
    console.log('使用路径下载:', row.downloadUrlByPath)
  }
  // 降级使用ID下载URL
  else if (row.downloadUrl) {
    window.open(row.downloadUrl, '_blank')
    console.log('使用ID下载:', row.downloadUrl)
  }
  // 兜底：使用ID构建下载URL
  else if (row.id) {
    window.open(`/api/files/download/${row.id}`, '_blank')
    console.log('使用ID下载（兜底）:', row.id)
  } else {
    ElMessage.error('无法获取下载地址')
  }
}

const handleDelete = (row: ToolFile) => {
  ElMessageBox.confirm('确定要删除这个文件吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteFile(row.id!)
      ElMessage.success('删除成功')
      loadData()
    } catch (error) {
      console.error('删除失败', error)
    }
  })
}

const handleDeleteFolder = (urlPath: string) => {
  ElMessageBox.confirm(`确定要删除该文件夹及其下所有文件吗？\n${urlPath}`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteFolder(urlPath)
      ElMessage.success('文件夹删除成功')
      loadData()
    } catch (error) {
      console.error('删除文件夹失败', error)
    }
  })
}

onMounted(() => {
  loadData()
  loadTools()
})
</script>

<style scoped>
.admin-files {
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

.upload-btn {
  padding: 12px 24px;
  border-radius: 10px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(0, 217, 255, 0.25);
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 217, 255, 0.35);
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

.text-gray {
  color: #909399;
  font-size: 12px;
}

.el-upload__tip {
  color: var(--text-secondary);
  font-size: 12px;
  margin-top: 8px;
}

.percentage-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-primary);
}

/* 进度条样式优化 */
.el-progress {
  margin-top: 10px;
}

[data-theme='dark'] .el-progress__text {
  color: var(--text-primary) !important;
}
</style>
