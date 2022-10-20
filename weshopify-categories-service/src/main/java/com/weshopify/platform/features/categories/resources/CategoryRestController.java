package com.weshopify.platform.features.categories.resources;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.weshopify.platform.features.categories.bean.CategoryBean;
import com.weshopify.platform.features.categories.bean.CategoryPageInfo;
import com.weshopify.platform.features.categories.domain.Category;
import com.weshopify.platform.features.categories.exceptions.CategoryNotFoundException;
import com.weshopify.platform.features.categories.service.CategoryService;


@RestController
public class CategoryRestController {

	@Autowired
	private CategoryService service;
	
	@Value("${file.storage.location}")
	private String fileStorageLocation;
	
	@PostMapping("/categories/check_unique")
	public String checkUnique(@Param("id") Integer id, @Param("name") String name,
			@Param("alias") String alias) {
		return service.checkUnique(id, name, alias);
	}
	
	@GetMapping("/categories")
	public ResponseEntity<List<Category>> listFirstPage(String sortDir) {
		return listByPage(1, sortDir, null);
	}
	
	@GetMapping("/categories/page/{pageNum}") 
	public ResponseEntity<List<Category>> listByPage(@PathVariable(name = "pageNum") int pageNum, 
			String sortDir,	String keyword) {
		if (sortDir ==  null || sortDir.isEmpty()) {
			sortDir = "asc";
		}
		
		CategoryPageInfo pageInfo = new CategoryPageInfo();
		List<Category> listCategories = service.listByPage(pageInfo, pageNum, sortDir, keyword);
		
		long startCount = (pageNum - 1) * CategoryService.ROOT_CATEGORIES_PER_PAGE + 1;
		long endCount = startCount + CategoryService.ROOT_CATEGORIES_PER_PAGE - 1;
		if (endCount > pageInfo.getTotalElements()) {
			endCount = pageInfo.getTotalElements();
		}
		
		return ResponseEntity.status(HttpStatus.OK).body(listCategories);		
	}
	
	
	@PostMapping(value="/categories",consumes = {MediaType.MULTIPART_FORM_DATA_VALUE,MediaType.APPLICATION_JSON_VALUE})
	public String saveCategory(@RequestPart String category, final @RequestParam("file") MultipartFile file) throws IOException {
		Category categoryEntity = new Category();
		if (!file.isEmpty()) {
			
			Path filePath = Paths.get(fileStorageLocation);
			  File f1 = new File(filePath+"/"+file.getOriginalFilename().substring(0, file.getOriginalFilename().indexOf('.'))
		    		  + UUID.randomUUID()+file.getOriginalFilename().substring(file.getOriginalFilename().indexOf('.')));
			  file.transferTo(f1);
			String fileName = StringUtils.cleanPath(file.getOriginalFilename());
			ObjectMapper mapper = new ObjectMapper();
			JsonParser jsonParser = mapper.createParser(category);
			CategoryBean catbean = mapper.readValue(jsonParser, CategoryBean.class);
			System.out.println(catbean.toString());
			BeanUtils.copyProperties(catbean, categoryEntity);
			categoryEntity.setImage(fileName);
			Category savedCategory = service.save(categoryEntity);
			String uploadDir = "category-images/" + savedCategory.getId();
			
			//AmazonS3Util.removeFolder(uploadDir);
			//AmazonS3Util.uploadFile(uploadDir, fileName, multipartFile.getInputStream());
		} else {
			
			BeanUtils.copyProperties(category, categoryEntity);
			service.save(categoryEntity);
		}
		
		//ra.addFlashAttribute("message", "The category has been saved successfully.");
		BeanUtils.copyProperties(categoryEntity, category);
		return category;
	}
	
	@PutMapping(value="/categories",consumes = {MediaType.MULTIPART_FORM_DATA_VALUE,MediaType.APPLICATION_JSON_VALUE})
	public CategoryBean updateCategory(@RequestPart String category, final @RequestParam("file") MultipartFile file) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonParser jsonParser = mapper.createParser(category);
		CategoryBean catBean = mapper.readValue(jsonParser, CategoryBean.class);
		System.out.println(catBean.toString());
		
		if (!file.isEmpty()) {
			
			Path filePath = Paths.get(fileStorageLocation);
			  File f1 = new File(filePath+"/"+file.getOriginalFilename().substring(0, file.getOriginalFilename().indexOf('.'))
		    		  + UUID.randomUUID()+file.getOriginalFilename().substring(file.getOriginalFilename().indexOf('.')));
			  file.transferTo(f1);
			String fileName = StringUtils.cleanPath(file.getOriginalFilename());
			
			//categoryEntity.setImage(fileName);
			//catbean = service.updateCategory(catbean);
			//String uploadDir = "category-images/" + savedCategory.getId();
			
			//AmazonS3Util.removeFolder(uploadDir);
			//AmazonS3Util.uploadFile(uploadDir, fileName, multipartFile.getInputStream());
			//return catBean;
		}
		catBean = service.updateCategory(catBean);
		return catBean;
	}
	
	@GetMapping(value="/categories/{id}/enabled/{status}")
	public ResponseEntity<Category> updateCategoryEnabledStatus(@PathVariable("id") Integer id,
			@PathVariable("status") boolean enabled, RedirectAttributes redirectAttributes) {
		Category categoryUpdated = service.updateCategoryEnabledStatus(id, enabled);
		String status = enabled ? "enabled" : "disabled";
		String message = "The category ID " + id + " has been " + status;
		redirectAttributes.addFlashAttribute("message", message);
		
		
		return ResponseEntity.status(HttpStatus.OK).body(categoryUpdated);
	}
	
	@GetMapping("/categories/delete/{id}")
	public ResponseEntity<List<Category>> deleteCategory(@PathVariable(name = "id") Integer id) {
		try {
			service.delete(id);
			
			
		} catch (CategoryNotFoundException ex) {
			
		}
		
		return listByPage(0, null, null);
	}
	
	/*
	 * @GetMapping("/categories/export/csv") public void
	 * exportToCSV(HttpServletResponse response) throws IOException { List<Category>
	 * listCategories = service.listCategoriesUsedInForm(); CategoryCsvExporter
	 * exporter = new CategoryCsvExporter(); exporter.export(listCategories,
	 * response); }
	 */
}
