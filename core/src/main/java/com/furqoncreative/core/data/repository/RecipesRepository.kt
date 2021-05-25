package com.furqoncreative.core.data.repository

import android.util.Log
import com.dicoding.tourismapp.core.data.Resource
import com.furqoncreative.core.data.NetworkBoundResource
import com.furqoncreative.core.data.source.local.LocalDataSource
import com.furqoncreative.core.data.source.remote.RemoteDataSource
import com.furqoncreative.core.data.source.remote.network.ApiResponse
import com.furqoncreative.core.data.source.remote.response.recipeslist.RecipesListItem
import com.furqoncreative.core.domain.model.recipes.Recipes
import com.furqoncreative.core.domain.repository.recipes.IRecipesRepository
import com.furqoncreative.core.utils.AppExecutors
import com.furqoncreative.core.utils.DataMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RecipesRepository(
    private val remoteDataSource: RemoteDataSource,
    private val localDataSource: LocalDataSource,
    private val appExecutors: AppExecutors
) : IRecipesRepository {

    override fun getAllRecipes(): Flow<Resource<List<Recipes>>> =
        object : NetworkBoundResource<List<Recipes>, List<RecipesListItem>>() {
            override fun loadFromDB(): Flow<List<Recipes>> {
                return localDataSource.getAllRecipes().map {
                    Log.d("TAG", "loadFromDB: $it ")
                    DataMapper.mapRecipesEntityToDomain(it)
                }
            }

            override fun shouldFetch(data: List<Recipes>?): Boolean = true

            override suspend fun createCall(): Flow<ApiResponse<List<RecipesListItem>>> =
                remoteDataSource.getAllRecipes()

            override suspend fun saveCallResult(data: List<RecipesListItem>) {
                val recipesList = DataMapper.mapRecipesResponsesToEntities(data)
                Log.d("TAG", "saveCallResult: $data")
                Log.d("TAG", "saveCallResult: $recipesList")
                localDataSource.insertRecipes(recipesList)
            }
        }.asFlow()

    override fun getFavoriteRecipes(): Flow<List<Recipes>> {
        return localDataSource.getAllRecipes().map {
            DataMapper.mapRecipesEntityToDomain(it)
        }
    }

    override fun setFavoriteRecipes(recipes: Recipes, state: Boolean) {
        val recipeEntity = DataMapper.mapRecipesDomainToEntity(recipes)
        appExecutors.diskIO().execute { recipeEntity?.let { localDataSource.setFavoriteRecipes(it, state) } }
    }
}

